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

/**
 * Class responsible for interacting with the IRC system itself, and keeping the connection alive.
 * The IrcConnection class owns a ConnectionThread sub-class which runs the actual connection on a
 * separate thread, given that we don't want to run it on the main thread.
 */
public class IrcConnection {
    /**
     * The maximum number of seconds which the main thread will wait for the IRC connection thread
     * to gracefully shut down the connection, before forcefully joining threads.
     */
    private final static int MAXIMUM_IRC_DISCONNECTION_TIME = 10;
    
    /**
     * Parameters which will be used to establish and maintain a connection with the IRC server.
     */
    public static class ConnectionParams {
        /**
         * The host or IP address though which the IRC server can be reached.
         */
        public String hostname;
        
        /**
         * The port number through which the IRC server can be reached. For most non-secured IRC
         * connections this will be 6667, although 6697 is common for secured connections.
         */
        public int port;
        
        /**
         * Whether SSL should be used when establishing the connection.
         */
        public boolean ssl;
        
        /**
         * Password required in order to connect to the IRC server.
         */
        public String password;
        
        /**
         * The nickname with which the bot should connect to IRC. Underscores will automatically be
         * appended to the nickname in case it's already taken.
         */
        public String nickname;
        
        /**
         * The password with which the user needs to authenticate with NickServ. This should not be
         * confused with the password required to connect to the server.
         */
        public String nickserv_password;
        
        /**
         * List of channels which the bot should automatically join when it has connected to the
         * IRC server, and is authenticated with NickServ.
         */
        public List<String> autojoin;
    }
    
    /**
     * Set of listeners which want to be notified of incoming events from IRC.
     */
    private final Set<IrcEventListener> mListeners;
    
    /**
     * Parameters using which the connection to IRC will be established and maintained.
     */
    private final ConnectionParams mConnectionParams;
    
    public IrcConnection(ConnectionParams connectionParams) {
        mListeners = new HashSet<IrcEventListener>();
        mConnectionParams = connectionParams;
    }
    
    /**
     * Connects to IRC by starting the connection thread. Attached IRC Event Listeners will be told
     * about whether the connection was established successfully through event delivery.
     */
    public void connect() {
        
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
