/**
 * Copyright (c) 2011 - 2014 Mineground, Las Venturas Playground
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.mineground.remote;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Class responsible for interacting with the IRC system itself, and keeping the connection alive.
 * The IrcConnection class owns a ConnectionThread sub-class which runs the actual connection on a
 * separate thread, given that we don't want to run it on the main thread.
 */
public class IrcConnection {
    /**
     * The maximum number of milliseconds which the main thread will wait for the IRC connection
     * thread to gracefully shut down the connection, before forcefully joining threads.
     */
    private final static int MAXIMUM_IRC_DISCONNECTION_TIME_MS = 10000; // 10 seconds
    
    /**
     * Parameters which will be used to establish and maintain a connection with the IRC server.
     */
    public static class ConnectionParams {
        /**
         * List of servers which we can try connecting to. Each entry in the list needs to be in
         * the format of "[hostname/ip]:[port]". The port may be prefixed with a plus ("+") to
         * indicate that a secured connection must be used.
         */
        public List<String> servers;
        
        /**
         * List of channels which the bot should automatically join when it has connected to the
         * IRC server, and is authenticated with NickServ. When a channel is protected with a
         * password, the password may follow the channel's name, separated by a space.
         */
        public List<String> channels;
        
        /**
         * The nickname with which the bot should connect to IRC. Underscores will automatically be
         * appended to the nickname in case it's already taken.
         */
        public String nickname;
        
        /**
         * Password required for the nickname to authenticate itself against NickServ, if (a) the
         * network is running Anope, and (b) the nickname has been registered.
         */
        public String password;
    }
    
    /**
     * Set of listeners which want to be notified of incoming events from IRC.
     */
    private final Set<IrcEventListener> mListeners;
    
    /**
     * The logger which will be used to output error messages and warnings from the IRC Connection.
     */
    private final Logger mLogger;
    
    /**
     * The Connection Thread contains the code required for actually establishing and maintaining
     * a connection to IRC, as well as the queues for communicating with the server thread.
     */
    private class ConnectionThread extends Thread {
        /**
         * Configuration through which the connection with IRC will be established. This information
         * is required every time we try to establish a connection with IRC.
         */
        private final ConnectionParams mConnectionParams;
        
        /**
         * Indicates whether a shutdown has been requested by the IRC Manager. This means that the
         * thread needs to send a QUIT message, and disconnect the socket.
         */
        private Boolean mShutdownRequested;
        
        /**
         * The socket through which the connection to IRC has been established. While the <code>
         * read()</code> operation blocks on the receiving side, we can call <code>send()</code> on
         * the server thread to flush data through.
         */
        private IrcSocket mSocket;

        private ConnectionThread(ConnectionParams connectionParams) {
            super("IrcConnectionThread");

            mConnectionParams = connectionParams;
            mShutdownRequested = false;
        }
        
        /**
         * The <code>run()</code> method is the main run-time of the IRC Connection Thread, which
         * will be executed on a thread different from the server's main thread.
         */
        @Override
        public void run() {
            mSocket = new IrcSocket(mConnectionParams.servers);
            while (!mShutdownRequested) {
                if (!mSocket.connect())
                    return; // TODO: Try to re-connect after some hold-off period.

                onConnectionEstablished();
                while (!mShutdownRequested && mSocket.isConnected()) {
                    final String message = mSocket.read();
                    if (message != null && message.length() > 0)
                        onIncomingMessage(message);
                }
                
                onConnectionLost();

                // TODO: Implement better logic for safely handling reconnections. Right now we'd
                //       just hammer the server, and it's better if we don't :-). 
                break;
            }
            
            mSocket.disconnect();
        }
        
        /**
         * Invoked by the connection runtime when a connection has been established. We need to send
         * the initial IRC identification commands here, to prevent being kicked from the server.
         */
        private void onConnectionEstablished() {
            mLogger.severe("[IRC] The connection with IRC has been established.");
            
            final String nickname = mConnectionParams.nickname;
            mSocket.send("USER " + nickname + " " + nickname + " - :" + nickname);
            mSocket.send("NICK " + nickname);
        }
        
        /**
         * Invoked when a message has been received from the IRC connection. Parse the message and
         * decide what needs to be done here, potentially handling it internally.
         *
         * @param message   The raw message which has been received from IRC.
         */
        private void onIncomingMessage(String rawMessage) {
            // TODO: Parse the message and then decide what to do with it.

            mLogger.info("[IRC] " + rawMessage);
        }
        
        /**
         * Invoked by the connection runtime when a previously established connection has been lost.
         */
        private void onConnectionLost() {
            mLogger.severe("[IRC] The connection with IRC has been closed.");
        }
        
        /**
         * Requests the IRC Connection thread to be shut down. This method should only be called
         * on the main server thread.
         */
        public void requestShutdown() {
            if (mSocket != null)
                mSocket.send("QUIT :Mineground has been unloaded.");

            mShutdownRequested = true;
        }
    }
    
    /**
     * The thread which will be used to asynchronously maintain a connection with the database for
     * this IrcConnection instance.
     */
    private final ConnectionThread mConnectionThread;
    
    public IrcConnection(ConnectionParams connectionParams) {
        mListeners = new HashSet<IrcEventListener>();
        mLogger = Logger.getLogger(getClass().getCanonicalName());
        mConnectionThread = new ConnectionThread(connectionParams);
    }
    
    /**
     * Connects to IRC by starting the connection thread. Attached IRC Event Listeners will be told
     * about whether the connection was established successfully through event delivery.
     */
    public void connect() {
        mConnectionThread.start();
    }
    
    /**
     * Sends <code>command</code> over the active IRC connection. <code>command</code> should be a
     * valid command per RFC2812, the updated IRC protocol.
     * 
     * @param command The command to send to the server.
     */
    public void send(String command) {
        
    }
    
    /**
     * Polls for pending incoming messages from the IRC thread. This method must be called from the
     * main server thread, as that's where events should be invoked.
     */
    public void doPollForMessages() {
        
    }
    
    /**
     * Synchronously disconnects from IRC if a connection has been established. A maximum number of
     * seconds, as identified by the <code>MAXIMUM_IRC_DISCONNECTION_TIME</code> constant.
     */
    public void disconnect() {
        mConnectionThread.requestShutdown();
        try {
            mConnectionThread.join(MAXIMUM_IRC_DISCONNECTION_TIME_MS);
        } catch (InterruptedException exception) {
            mLogger.severe("The IRC Connection thread could not be shut down: " + exception.getMessage());
        }
    }
    
    /**
     * Adds <code>listener</code> as an object which should be informed of IRC events when they
     * occur. Each listener can only be registered once.
     * 
     * @param listener The listener which should receive IRC events.
     */
    public void addListener(IrcEventListener listener) {
        mListeners.add(listener);
    }
    
    /**
     * Removes <code>listener</code> from the set of objects to inform of events.
     * 
     * @param listener The object which will no longer receive events.
     */
    public void removeListener(IrcEventListener listener) {
        mListeners.remove(listener);
    }
}
