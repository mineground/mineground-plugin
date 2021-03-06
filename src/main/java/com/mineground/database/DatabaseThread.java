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

package com.mineground.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The DatabaseThread is the thread which actually communicates with the MySQL database. We run
 * this on a separate thread since queries should not block the rest of the server.
 */
public class DatabaseThread extends Thread {
    /**
     * The maximum number of *seconds* any individual query may take whilst executing. After this
     * duration the query will be considered as having failed.
     */
    private final static int MAXIMUM_QUERY_EXECUTION_TIME = 10;

    /**
     * Connection parameters which are being used to connect to the database. This field will be
     * assigned to once (in the constructor) and should be considered immutable thereafter.
     */
    private final DatabaseConnectionParams mConnectionParams;
    
    /**
     * Whether the database thread should be shut down. This means that no further queries will
     * be accepted. All pending queries will be flushed before exiting.
     */
    private boolean mShutdownRequested;
    
    /**
     * A blocking queue which contains the queries which are currently pending execution.
     */
    private final LinkedBlockingQueue<PendingQuery> mPendingQueryQueue;
    
    /**
     * A blocking queue which contains the queries which have already been executed, and can
     * be finalized by the main thread by invoking the pending promises on them.
     */
    private final LinkedBlockingQueue<PendingQuery> mFinishedQueryQueue;
    
    /**
     * Logger used for outputting warnings and errors occurring on the database thread.
     */
    private final Logger mLogger;
    
    /**
     * The database connection which will be servicing this database thread.
     */
    private Connection mConnection;
    
    /**
     * Exception which will be thrown by the executeQuery() method when it notices that the open
     * connection with the database server has been lost.
     */
    private class ConnectionLostException extends Exception {
        private static final long serialVersionUID = -8502977559194885378L;
    }
    
    public DatabaseThread(DatabaseConnectionParams connectionParams) {
        mConnectionParams = connectionParams;
        
        mLogger = Logger.getLogger(getClass().getCanonicalName());
        mShutdownRequested = false;
        mPendingQueryQueue = new LinkedBlockingQueue<PendingQuery>();
        
        // TODO: LinkedBlockingQueue probably isn't the right type for the finished queue.
        mFinishedQueryQueue = new LinkedBlockingQueue<PendingQuery>();
    }
    
    /**
     * Main loop for the database thread. It will do a best effort job in keeping a connection
     * alive, or re-establishing it when the connection has been lost. Pending queries will then
     * be retrieved from the |mPendingQueryQueue|, which will be executed and then stored in
     * the |mFinishedQueryQueue| so that the main thread can run off with the results.
     */
    public void run() {
        int reconnectionBackoffExponent = 0, reconnectionBackoffSeconds = 0;
        while (!mShutdownRequested) {
            if (mConnection == null) {
                try {
                    reconnectionBackoffExponent = Math.min(reconnectionBackoffExponent, /** 2 ^ 7 == 128 **/ 7);
                    reconnectionBackoffSeconds = (int) Math.pow(2, reconnectionBackoffExponent);
                    
                    mLogger.info("Waiting " + reconnectionBackoffSeconds + " seconds before reconnecting to the database...");
                    Thread.sleep(1000 * reconnectionBackoffSeconds);

                } catch (InterruptedException e) { /** It's safe to ignore this exception **/ }
                
                if (!connect()) {
                    reconnectionBackoffExponent++;
                    continue;
                }
                
                // The connection has succeeded, so make sure that the reconnection back off
                // exponent is set to zero, meaning that we can reconnect immediately again next
                // time the connection is lost (which hopefully is never?).
                reconnectionBackoffExponent = 0;
            }

            try {
                PendingQuery query = mPendingQueryQueue.poll(1, TimeUnit.SECONDS);
                if (query == null)
                    continue;
                
                mFinishedQueryQueue.add(executeQuery(query));

            } catch (InterruptedException exception) {
                /** It's safe to ignore this exception **/
            } catch (ConnectionLostException exception) {
                mConnection = null;
            }
        }
        
        // Flush the queries which are still in the queue, instead of disregarding them
        // altogether, which may lead to a loss of user data. At this point we discard any
        // pending SELECT queries, given that it's likely their features are gone already.
        if (mPendingQueryQueue.size() > 0) {
            mLogger.info("Shutting down database thread.. flushing " + mPendingQueryQueue.size() + " queries.");
            while (mPendingQueryQueue.size() > 0) {
                PendingQuery query = mPendingQueryQueue.poll();
                if (query == null || query.query.startsWith("SELECT"))
                    continue;
                
                try {
                    executeQuery(query);

                } catch (ConnectionLostException e) {
                    // If we *did* lose connection at this point, we're just going to give up. It's
                    // likely that the plugin is being reloaded because of database connectivity
                    // issues as it is, and reconnecting would just block the database further.
                    mLogger.severe("Could not cleanly shut down the database thread. Queries lost.");
                    return;
                }
            }
        }

        mPendingQueryQueue.clear();
        mFinishedQueryQueue.clear();
        mShutdownRequested = false;
        
        disconnect();
    }
    
    /**
     * Executes |query| on the established database connection. If a MySQL error occurs, the
     * PendingQuery's error message will be set to the textual explanation. Otherwise a new
     * DatabaseResult object will be created, containing the result values.
     * 
     * @param query The query which needs to be executed on the database.
     * @return      The same query, but in a finished state.
     */
    private PendingQuery executeQuery(PendingQuery query) throws ConnectionLostException {
        boolean executed = false;
        try {
            final PreparedStatement statement = mConnection.prepareStatement(query.query, Statement.RETURN_GENERATED_KEYS);
            final DatabaseResult result = new DatabaseResult();
            
            statement.setQueryTimeout(MAXIMUM_QUERY_EXECUTION_TIME);
            if (query.parameters != null) {
                // Apply all parameters to the prepared statement depending on their type. Only
                // the types supported in DatabaseStatement will be inserted here. If an entry
                // with an unknown type is occurred, execution of this query will be aborted.
                for (int parameterIndex : query.parameters.keySet()) {
                    Object parameter = query.parameters.get(parameterIndex);
                    
                    if (parameter instanceof String)
                        statement.setString(parameterIndex, (String) parameter);
                    else if (parameter instanceof Long)
                        statement.setLong(parameterIndex, (Long) parameter);
                    else if (parameter instanceof Double)
                        statement.setDouble(parameterIndex, (Double) parameter);
                    else {
                        query.error = "Invalid query parameter supplied at index " + parameterIndex;
                        return query;
                    }
                }
            }

            if (statement.execute()) {
                executed = true; // so that we don't accidentially run the query again.

                ResultSet resultSet = statement.getResultSet();
                ResultSetMetaData meta = statement.getMetaData();
                
                List<String> columnNames = new ArrayList<String>();
                
                int columnCount = meta.getColumnCount();
                for (int columnIndex = 1; columnIndex <= columnCount; ++columnIndex)
                    columnNames.add(meta.getColumnName(columnIndex));
                
                result.setColumnNames(columnNames);

                while (resultSet.next()) {
                    DatabaseResultRow resultRow = new DatabaseResultRow(result, columnCount);
                    for (int columnIndex = 1; columnIndex <= columnCount; ++columnIndex)
                        resultRow.add(resultSet.getObject(columnIndex));

                    result.rows.add(resultRow);
                }
            } else {
                executed = true; // so that we don't accidentially run the query again.

                result.affectedRows = statement.getUpdateCount();
                
                ResultSet generatedKeysResultSet = statement.getGeneratedKeys();
                if (generatedKeysResultSet.next())
                    result.insertId = generatedKeysResultSet.getInt(1);
            }
            
            query.result = result;
            
        } catch (SQLException exception) {
            // If the connection has been lost, queue the query for execution again if necessary
            // and throw a ConnectionLostException allowing the thread to reconnect itself.
            if (isErrorCodeConnectionLost(exception)) {
                if (!executed)
                    mPendingQueryQueue.add(query);
                
                throw new ConnectionLostException();
            }

            String message = "Error while executing the MySQL query (" + 
                    exception.getErrorCode() + "): " + exception.getMessage();

            query.error = message;
        }
        
        return query;
    }
    
    /**
     * If we can recognize the |exception| thrown by the database driver as something which means
     * that the connection has been lost, Mineground should automatically reconnect to the server.
     * 
     * @param exception The SQLException to read the SQL state from.
     * @return          True if the error code indicates that the connection was lost.
     */
    private boolean isErrorCodeConnectionLost(SQLException exception) {
        // 08S01 represents a "communication link failure", e.g. connection timed out.
        if (exception.getSQLState() == "08S01" /** communication link failed **/ ||
            exception.getSQLState() == "08003" /** connection implicitly closed **/)
            return true;

        // TODO: Remove this once reconnecting is relatively stable.
        mLogger.severe("SQL State: " + exception.getSQLState());
        
        return false;
    }
    
    /**
     * Connects to the database, and returns whether the connection was successful. This method
     * will output error messages to the logger if it couldn't establish a new connection.
     *
     * @return Whether the connection to the database was successful.
     */
    private boolean connect() {
        String connectionUrl = "jdbc:mysql://" + mConnectionParams.hostname + ":" +
                mConnectionParams.port + "/" + mConnectionParams.database;
        
        try {
            mConnection = DriverManager.getConnection(connectionUrl, mConnectionParams.username, mConnectionParams.password);
            mLogger.info("Mineground has established a connection with the database!");

            return true;
        } catch (SQLException exception) {
            String message = "Could not connect to " + mConnectionParams.username + "@" +
                    mConnectionParams.hostname + ":" + mConnectionParams.port +
                    " for database " + mConnectionParams.database;
            
            message += " (" + exception.getErrorCode() + "): " + exception.getMessage();
            mLogger.severe(message);
        }
        
        return false;
    }
    
    /**
     * Closes the established connection with the database, if it's still active. This normally
     * happens when the database thread is being terminated.
     */
    private void disconnect() {
        if (mConnection == null)
            return;
        
        try {
            mConnection.close();
        } catch (SQLException e) { /** It's safe to ignore this exception **/ }

        mLogger.info("Mineground has closed the connection with the database!");
        mConnection = null;
    }
    
    /**
     * Requests a shutdown of the database thread. After all currently queued queries have
     * finished, this thread will automatically exit.
     */
    public void requestShutdown() {
        mShutdownRequested = true;
    }
    
    /**
     * Enqueues |pendingQuery| to be executed on the database thread.
     * 
     * @param pendingQuery The query which should be executed on the database.
     */
    public void enqueue(PendingQuery pendingQuery) {
        mPendingQueryQueue.add(pendingQuery);
    }
    
    /**
     * Immediately returns the PendingQuery object of a finished query if one is available. The
     * name emphasizes the fact that we will not block the main thread on this.
     *
     * @return A finished query, or NULL.
     */
    public PendingQuery immediatelyRetrieveFinishedQuery() {
        return mFinishedQueryQueue.poll();
    }
}
