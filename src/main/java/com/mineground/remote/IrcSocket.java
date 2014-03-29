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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The IRC Socket class owns the lowest level functionality related to the IRC connection needed
 * for Mineground's IRC bot: the actual connection. It maintains a list of servers it can connect
 * to, and sanitizes input from and output to sockets with those servers.
 */
public class IrcSocket {
    /**
     * The Logger which will be used to output diagnostic information about the socket.
     */
    private final Logger mLogger;
    
    /**
     * Class representing the information stored about each server with this socket can connect to.
     */
    private class ServerInfo {
        public String address;
        public int port;
        public boolean ssl;
        public String password;
        
        public ServerInfo() {
            address = "";
            port = 6667;
            ssl = false;
            password = "";
        }
    }
    
    /**
     * List of the servers which this socket can try connecting to.
     */
    private final Set<ServerInfo> mServers;
    
    public IrcSocket(List<String> servers) {
        mLogger = Logger.getLogger(getClass().getCanonicalName());
        mServers = new HashSet<ServerInfo>(servers.size());
        for (String server : servers) {
            List<ServerInfo> serverInfo = resolveServerEntry(server);
            if (serverInfo.size() > 0)
                mServers.addAll(serverInfo);
        }
    }
    
    /**
     * Synchronously establishes a connection with the IRC server and returns whether the connection
     * succeeded. User authentication explicitly is not part of the initial connection process.
     * 
     * @return Whether an socket connection has been established with one of the IRC servers.
     */
    public boolean connect() {
        for (ServerInfo server : mServers) {
            mLogger.info("IRC Server [" + server.address + ":" + server.port + "] SSL=" + (server.ssl ? "true" : "false"));
        }
        
        return false;
    }
    
    /**
     * Parses and resolves <code>server</code> into a list of ServerInfo structures.
     * 
     * This method will first parse <code>server</code> to read the individual components from the
     * string, which must be in the format of "[hostname/ip]:[+]?[port][ password]?".
     * 
     * If the server entry is defined using a hostname rather than an IP address, the hostname will
     * be resolved into a list of IPv4 addresses which represent it. Each of these addresses will
     * get its own ServerInfo instance. Alternatively, if the server entry is defined using an IP
     * address, only a single ServerInfo instance will be returned.
     * 
     * @param server The string describing this server entry.
     */
    private List<ServerInfo> resolveServerEntry(String serverEntry) {
        String[] serverDefinition = serverEntry.split("[:\\s]", 3);
        
        final List<ServerInfo> servers = new ArrayList<ServerInfo>();
        
        ServerInfo server = new ServerInfo();
        switch (serverDefinition.length) {
            case 0:
                mLogger.severe("Invalid IRC server configuration supplied: \"" + serverEntry + "\".");
                return servers;

            case 3: // the hostname, port and password have been specified.
                server.password = serverDefinition[2];
            case 2: // the hostname and port have been specified.
                if (serverDefinition[1].startsWith("+")) {
                    serverDefinition[1] = serverDefinition[1].substring(1);
                    server.ssl = true;
                }
                server.port = Integer.parseInt(serverDefinition[1]);
            case 1: // only the hostname has been specified.
                server.address = serverDefinition[0];
        }
        
        // TODO: Determine whether the server is a hostname or an IP address. When it's the former,
        //       resolve the hostname into a list of IP addresses.
        servers.add(server);
        
        return servers;
    }
}
