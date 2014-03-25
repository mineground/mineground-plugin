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

import java.util.LinkedList;
import java.util.List;

/**
 * Class responsible for interacting with the IRC system itself, and keeping the connection alive.
 * The IrcConnection class owns a ConnectionThread sub-class which runs the actual connection on a
 * separate thread, given that we don't want to run it on the main thread.
 */
public class IrcConnection {
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
     * List of listeners which want to be notified of incoming events from IRC.
     */
    private final List<IrcEventListener> mListeners;
    
    public IrcConnection(ConnectionParams connectionParams) {
        mListeners = new LinkedList<IrcEventListener>();
        
        // TODO: Start the connection thread.
    }
    
    public void connect() {
    }
    
    public void disconnect() {
    }
}
