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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * The IRC Socket class owns the lowest level functionality related to the IRC connection needed
 * for Mineground's IRC bot: the actual connection. It maintains a list of servers it can connect
 * to, and sanitizes input from and output to sockets with those servers.
 */
public class IrcSocket {
    /**
     * The default quit message which will be send to IRC right before actually closing the output
     * stream to the socket. No guarantees will be made that this actually arrives.
     */
    private final static String DEFAULT_QUIT_COMMAND = "QUIT :Mineground has been unloaded.";
    
    /**
     * Maximum number of milliseconds which reading from a socket may block for. After this period
     * of time a SocketTimeoutException will be thrown, causing IrcSocket.read() to return.
     */
    private final static int SOCKET_TIMEOUT_MS = 1000;

    /**
     * The Logger which will be used to output diagnostic information about the socket.
     */
    private final Logger mLogger;
    
    /**
     * Class representing the information stored about each server with this socket can connect to.
     */
    private class ServerInfo {
        public InetAddress address;
        public int port;
        public boolean ssl;
        public String password;
        
        public ServerInfo() {
            address = null;
            port = 6667;
            ssl = false;
            password = "";
        }
        
        public ServerInfo(ServerInfo clone) {
            address = clone.address;
            port = clone.port;
            ssl = clone.ssl;
            password = clone.password;
        }
        
        /**
         * Naive hashCode() algorithm which should roughly divide ServerInfo entries in separate
         * buckets when storing instances in a Set.
         */
        public int hashCode() {
            return (address == null ? 0 : address.hashCode()) +
                   (ssl ? 1 : 0) + password.hashCode() + port;
        }
        
        /**
         * Strict equality method for checking whether two ServerInfo instances describe the same
         * server. The double-equals operator in Java ("==") doesn't do that for us.
         */
        public boolean equals(Object otherObj) {
            if (otherObj == null || !(otherObj instanceof ServerInfo))
                return false;
            
            final ServerInfo other = (ServerInfo) otherObj;
            return this.address.equals(other.address) &&
                   this.password.equals(other.password) &&
                   this.port == other.port &&
                   this.ssl == other.ssl;
        }
    }
    
    /**
     * Set of the servers which this socket can try connecting to. Because hostnames in server
     * entries will be resolved to the IP addresses they map to, it would be possible to get
     * duplicates, and using a set allows us to avoid that.
     */
    private final Set<ServerInfo> mServers;
    
    /**
     * Information about the server to which the socket is currently connected.
     */
    private ServerInfo mServer;
    
    /**
     * The actual underlying Socket which will power this implementation. The <code>connect()</code>
     * method will ensure that this is the right kind of socket, as we need an SSLSocket instance
     * when dealing with a server that requires a secured connection.
     */
    private Socket mSocket;
    
    /**
     * The input stream from which data can be read when <code>mSocket</code> describes an
     * established connection with an IRC server.
     */
    private BufferedReader mSocketInputReader;
    
    /**
     * The output stream to which data should be send when <code>mSocket</code> describes an
     * established connection with an IRC server.
     */
    private OutputStreamWriter mSocketOutputWriter;
    
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
        mServer = selectServer();
        if (mServer == null) {
            mLogger.severe("Unable to establish a connection to IRC: no servers have been defined.");
            return false;
        }
        
        try {
            final SocketFactory socketFactory = mServer.ssl ?
                    getSecureSocketFactory() : SocketFactory.getDefault();
            
            mSocket = socketFactory.createSocket(mServer.address, mServer.port);
            mSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
            
            mSocketInputReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            mSocketOutputWriter = new OutputStreamWriter(mSocket.getOutputStream(), "UTF-8");
            return true;

        } catch (IOException | KeyManagementException | NoSuchAlgorithmException exception) {
            mLogger.severe("Unable to establish a connection to IRC: " + exception.getMessage());
        }

        return false;
    }
    
    /**
     * Returns whether the socket curated by this IrcSocket is still alive and connected.
     * 
     * @return Whether the socket is still alive and connected.
     */
    public boolean isConnected() {
        return mSocket != null && !mSocket.isClosed();
    }
    
    /**
     * Returns a list with new incoming messages which we can read from the input stream. 
     * 
     * @return A list with newly read messages.
     */
    public String read() {
        if (mSocketInputReader == null)
            return null;

        try {
            final String inputLine = mSocketInputReader.readLine();
            if (inputLine != null)
                return inputLine;
            
            // Since |inputLine| is NULL, an end-of-file has been detected on the socket, which
            // means that it has disconnected.
            disconnect();

        } catch (SocketTimeoutException exception) {
            // A SocketTimeoutException occurs when the read timeout for this socket has passed,
            // which means that no new data is available right now. This is safe to ignore.
            return null;

        } catch (IOException e) { /** Only SocketTimeoutException should be thrown.  **/ } 

        return null;
    }
    
    /**
     * Sends <code>command</code> directly to the IRC server. A boolean will be returned which
     * indicates whether the command could be sent successfully.
     * 
     * @param command   The command to send to the IRC server.
     * @return          Whether the command was sent successfully.
     */
    public boolean send(String command) {
        if (mSocketOutputWriter == null)
            return false;
        
        try {
            synchronized (mSocketOutputWriter) {
                mSocketOutputWriter.write(command.trim() + "\n");
                mSocketOutputWriter.flush();
            }

            return true;

        } catch (IOException exception) {
            mLogger.severe("The connection with the IRC server has been lost: " + exception.getMessage());

            // Since the connection has been lost, make sure that |mSocketOutputWriter| is not used
            // anymore, and then disconnect from the server entirely.
            synchronized (mSocketOutputWriter) {
                mSocketOutputWriter = null;
            }

            disconnect();
        }

        return false;
    }
    
    /**
     * Disconnects from the IRC server and resets all members which are related to the established
     * connection to NULL. <code>connect()</code> must be called before communication can resume.
     */
    public void disconnect() {
        send(DEFAULT_QUIT_COMMAND);
        try {
            if (mSocketOutputWriter != null)
                mSocketOutputWriter.close();
        
            if (mSocketInputReader != null)
                mSocketInputReader.close();
            
            if (mSocket != null)
                mSocket.close();
            
        } catch (IOException exception) {
            // Exceptions are deliberately ignored at this point.
            
        } finally {
            mSocketOutputWriter = null;
            mSocketInputReader = null;
            mSocket = null;
            mServer = null;
        }
    }
    
    /**
     * Returns the server to which the socket is currently connected.
     * 
     * @return The server to which the socket is currently connected.
     */
    public ServerInfo getServer() {
        return mServer;
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
        String serverAddress = "";
        
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
                serverAddress = serverDefinition[0];
        }
        
        try {
            // Attempt to resolve all IP addresses which the defined address resolves for. Then
            // create a new entry in |servers| for each of the resolved IP addresses.
            InetAddress[] addresses = InetAddress.getAllByName(serverAddress);
            for (InetAddress resolvedAddress : addresses) {
                ServerInfo resolvedServer = new ServerInfo(server);
                resolvedServer.address = resolvedAddress;
                
                servers.add(resolvedServer);
            }
            
        } catch (UnknownHostException e) {
            mLogger.severe("Unable to resolve the IP address for IRC server: \"" + serverEntry + "\".");
            return servers;
        }
        
        return servers;
    }
    
    /**
     * Returns a SocketFactory which can be used to establish secured connections with any of the
     * IRC servers. Since most IRC networks use self-signed certificates for their servers, we'll
     * create an X509 trust manager which trusts these certificates.
     * 
     * @return                           A secure socket factory accepting self-signed certificates.
     * @throws NoSuchAlgorithmException  When the default Trust Manager algorithm does not exist.
     * @throws KeyManagementException    When the SSL Context could not be initialized.
     */
    private SocketFactory getSecureSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        // Use our own trust manager, which accepts any kind of SSL certificate as valid. Many IRC
        // networks use self-signed, sometimes even expired certificates using which clients create
        // "secured" connections. The connection will still benefit from encryption, although
        // technically a MITM attack won't be avoided this way.
        TrustManager[] trustManagerArray = new TrustManager[] {
                new MinegroundX509TrustManager()
        };

        // Initialize the SSL context with with secure base-randomness.
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(new KeyManager[0], trustManagerArray, new SecureRandom());
        
        // Return the socket factory which can be created based on that context.
        return context.getSocketFactory();
    }
    
    /**
     * Selects a server from the <code>mServers</code> set which the upcoming connection should be
     * attempted to. In the current implementation this returns a random server.
     * 
     * @return An appropriate server from the <code>mServers</code> set.
     */
    private ServerInfo selectServer() {
        if (mServers.size() == 0)
            return null;

        // Pick a random server from the list of resolved servers in |mServers|. This can later
        // evolve to be a more advanced algorithm and take variables such as failure rate and
        // latency into account when selecting an appropriate server.
        return (ServerInfo) mServers.toArray()[(new Random()).nextInt(mServers.size())];
    }
    
    /**
     * X509 certificate trust manager which accepts certificates which have been published by a
     * non-trusted root, for example self signed certificates. Not implementing the methods means
     * that no CertificateException exception will be thrown, therefore passing validation.
     */
    private class MinegroundX509TrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
            // TODO: Should we at least validate the date here? Is even that too much? Need to get
            //       some data based on what IRC servers these days do.
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
