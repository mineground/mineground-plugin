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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.bukkit.Server;

import com.mineground.remote.IrcMessage.Origin;

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
         * The nickname with which the bot should connect to IRC. A random number will automatically
         * be appended to the nickname in case it's already taken.
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
     * Information about a received message from the IRC thread, which has to be announced on the
     * server thread's attached event listeners.
     */
    private class ReceivedMessage {
        public IrcUser user;
        public String destination;
        public String message;
        
        public ReceivedMessage(IrcUser user_, String destination_, String message_) {
            user = user_;
            destination = destination_;
            message = message_;
        }
    }
    
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
         * Because IrcUsers are also CommandSenders internally, the connection thread needs the
         * instance of the server to support user creation. No methods will be called on mServer.
         * 
         * TODO: Remove the dependency on <code>mServer</code> as soon as that's feasible.
         */
        private final Server mServer;
        
        /**
         * A blocking queue which contains messages which have been received, but haven't yet been
         * forwarded to event listeners on the server thread.
         */
        private final LinkedBlockingQueue<ReceivedMessage> mReceivedMessageQueue;
        
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
        
        /**
         * Map of users currently online on IRC, and the information we know about them.
         */
        private final Map<String, IrcUser> mUsers;
        
        /**
         * Nickname using which the connection has been established. The nickname can be changed
         * either by the server (forced nickname change), or by us (NICK command).
         */
        private String mNickname;

        private ConnectionThread(ConnectionParams connectionParams, Server server) {
            super("IrcConnectionThread");

            mConnectionParams = connectionParams;
            mServer = server;

            mReceivedMessageQueue = new LinkedBlockingQueue<ReceivedMessage>();
            mShutdownRequested = false;
            mUsers = new HashMap<String, IrcUser>();
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
            
            mNickname = nickname;
        }
        
        /**
         * Invoked when a message has been received from the IRC connection. Parse the message and
         * decide what needs to be done here, potentially handling it internally.
         *
         * @param message   The raw message which has been received from IRC.
         */
        private void onIncomingMessage(String incomingMessage) {
            IrcMessage message;
            try {
                // Since we're dealing with user input, it is possible that the message type is
                // malformed. When that happens, discard the individual message rather than crashing
                // the entire IRC connection thread.
                message = IrcMessage.Parse(incomingMessage);
                if (message == null)
                    return;
                
            } catch (Exception e) { 
                mLogger.severe("[IRC] Unable to parse an IRC message: " + incomingMessage);
                e.printStackTrace();
                return;
            }

            switch(message.getType()) {
                // The WELCOME message will be send by the IRC server when client registration has
                // succeeded. Identify with NickServ 
                case WELCOME:
                    if (mConnectionParams.password != null && mConnectionParams.password.length() > 0)
                        send("PRIVMSG NickServ :IDENTIFY " + mConnectionParams.password);

                    break;

                // When the Message Of The Day (MOTD) has completed, auto-join the list of channels
                // which the bot has been configured to join.
                case MOTD_END:
                    for (String channel : mConnectionParams.channels)
                        send("JOIN " + channel);

                    break;
                
                // Message indicating that the nickname chosen by this bot is already in use on the
                // IRC server. Append a random number 
                case NICKNAME_IN_USE:
                    mNickname = mConnectionParams.nickname + ((new Random()).nextInt(899) + 100);
                    send("NICK " + mNickname);

                    break;

                // Most incoming messages from users on IRC will be PRIVMSGs, definitely the ones
                // which the Mineground mod will recognize.
                case PRIVMSG:
                    mReceivedMessageQueue.add(new ReceivedMessage(getUserForMessage(message),
                                                                  message.getDestination(),
                                                                  message.getText()));
                    
                    break;
                    
                // When the server sends a PING, it wants the client to indicate that it's still
                // alive by replying with a PONG message containing the same text.
                case PING:
                    send("PONG :" + message.getText());
                    break;
                
                // Indication from the server that a fatal error has occurred, and the connection
                // needs to be closed. Forcefully disconnect the socket on our end.
                case ERROR:
                    mSocket.disconnect();
                    break;

                // The following message types are understood, but ignored by default.
                case NOTICE:
                    break;

                default:
                    mLogger.info("[IRC] Previous message couldn't be recognized!");
                    break;
            }
        }
        
        /**
         * Invoked by the connection runtime when a previously established connection has been lost.
         */
        private void onConnectionLost() {
            mLogger.severe("[IRC] The connection with IRC has been closed.");
        }
        
        /**
         * Returns the <code>IrcUser</code> instance for the source of <code>message</code>.
         * 
         * @param message The message to get the IrcUser object for.
         * @return        The IrcUser object for the source of this message.
         */
        private IrcUser getUserForMessage(IrcMessage message) {
            final String nickname = message.getOrigin().getNickname();
            if (!mUsers.containsKey(nickname)) {
                // TODO: We need to have access to the Bukkit Server here. When generalizing the IRC
                //       sub-system of Mineground, we should probably introduce a IrcUserFactory or
                //       something similar, allowing the embedder to create the objects.
                mUsers.put(nickname, new IrcUser(mServer, IrcConnection.this, message.getOrigin()));
            }
            
            final IrcUser user = mUsers.get(nickname);
            user.updateOriginIfNeeded(message.getOrigin());
            
            return user;
        }
        
        /**
         * Sends <code>command</code> to the server if the connection has been established. If it
         * hasn't, the message will be silently ignored.
         * 
         * @param command Command to send to over established connection.
         */
        public void send(String command) {
            if (mSocket == null)
                return;

            mSocket.send(command);
        }
        
        /**
         * Immediately retrieves a ReceivedMessage instance if any has been queued, or returns NULL
         * in case there hasn't. The server thread will call this method to receive any pending
         * incoming messages, so that we can handle them on the server thread.
         *
         * @return A received message, or NULL.
         */
        public ReceivedMessage immediatelyRetrieveReceivedMessage() {
            return mReceivedMessageQueue.poll();
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
    
    // TODO: The IrcConnection class shouldn't know about Server.
    public IrcConnection(ConnectionParams connectionParams, Server server) {
        mListeners = new HashSet<IrcEventListener>();
        mLogger = Logger.getLogger(getClass().getCanonicalName());
        mConnectionThread = new ConnectionThread(connectionParams, server);
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
        // ConnectionThread::send() has been factored in a way to make it as independent as possible
        // from the rest of the thread, so this should be relatively safe to use.
        //
        // No, honestly, it's stupid. But I'll need much more time to properly refactor the IRC
        // system (which I don't have right now), and I don't understand Java's silly socket I/O
        // blocking model enough yet to design a nice, completely async interface.
        mConnectionThread.send(command);
    }
    
    /**
     * Polls for pending incoming messages from the IRC thread. This method must be called from the
     * main server thread, as that's where events should be invoked.
     */
    public void doPollForMessages() {
        ReceivedMessage message = mConnectionThread.immediatelyRetrieveReceivedMessage();
        while (message != null) {
            message.user.updateDestination(message.destination);
            for (IrcEventListener listener : mListeners)
                listener.onMessageReceived(message.user, message.destination, message.message);
            
            message = mConnectionThread.immediatelyRetrieveReceivedMessage();
        }
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
