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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.mineground.base.Promise;
import com.mineground.base.PromiseError;

// Implementation of the DatabaseConnection interface, based on a JDBC connection using the MySQL
// J/Connection connector. A thread is used for asynchronous communication with the database.
public class DatabaseConnectionImpl implements DatabaseConnection {
    // The maximum number of *milliseconds* which the main thread will be waiting on the database
    // thread to shutdown cleanly. When this expires, the database thread will be considered dead.
    private final static int MAXIMUM_DISCONNECT_WAIT_TIME = 5000;
    
    // Logger used for outputting warnings and errors occurring on the database connection. The
    // instance will be used from both the main and database threads, and is guaranteed to be safe.
    private final Logger mLogger;
    
    // The DatabaseThread is the thread which actually communicates with the MySQL database. We run
    // this on a separate thread since queries should not block the rest of the server.
    private class DatabaseThread extends Thread {
        // Connection parameters which are being used to connect to the database. This field will be
        // assigned to once (in the constructor) and should be considered immutable thereafter.
        private final DatabaseConnectionParams mConnectionParams;
        
        // Whether the database thread should be shut down. This means that no further queries will
        // be accepted. All pending queries will be flushed before exiting.
        private boolean mShutdownRequested;
        
        // A blocking queue which contains the queries which are currently pending execution.
        private final LinkedBlockingQueue<PendingQuery> mPendingQueryQueue;
        
        // A blocking queue which contains the queries which have already been executed, and can
        // be finalized by the main thread by invoking the pending promises on them.
        private final LinkedBlockingQueue<PendingQuery> mFinishedQueryQueue;
        
        // The database connection which will be servicing this database thread.
        private Connection mConnection;
        
        public DatabaseThread(DatabaseConnectionParams connectionParams) {
            mConnectionParams = connectionParams;
            mShutdownRequested = false;
            mPendingQueryQueue = new LinkedBlockingQueue<PendingQuery>();
            
            // TODO: LinkedBlockingQueue probably isn't the right type for the finished queue.
            mFinishedQueryQueue = new LinkedBlockingQueue<PendingQuery>();
        }
        
        // Main loop for the database thread. It will do a best effort job in keeping a connection
        // alive, or re-establishing it when the connection has been lost. Pending queries will then
        // be retrieved from the |mPendingQueryQueue|, which will be executed and then stored in
        // the |mFinishedQueryQueue| so that the main thread can run off with the results.
        public void run() {
            connect();
            
            while (!mShutdownRequested) {
                try {
                    PendingQuery query = mPendingQueryQueue.poll(1, TimeUnit.SECONDS);
                    if (query == null)
                        continue;
                    
                    mFinishedQueryQueue.add(executeQuery(query));
                } catch (InterruptedException e) { /** It's safe to ignore this exception **/ }
            }

            mPendingQueryQueue.clear();
            mFinishedQueryQueue.clear();
            mShutdownRequested = false;
            
            disconnect();
        }
        
        // Executes |query| on the established database connection. If a MySQL error occurs, the
        // PendingQuery's error message will be set to the textual explanation. Otherwise a new
        // DatabaseResult object will be created, containing the result values.
        private PendingQuery executeQuery(PendingQuery query) {
            try {
                final Statement statement = mConnection.createStatement();
                final DatabaseResult result = new DatabaseResult();
                
                if (statement.execute(query.query, Statement.RETURN_GENERATED_KEYS)) {
                    ResultSet resultSet = statement.getResultSet();
                    while (resultSet.next()) {
                        // TODO: Store the retrieved information in the |result| object, so that the
                        //       caller can actually use it. We can't pass ResultSet back here,
                        //       because it depends on the statement and the connection.

                        result.numRows++;
                    }
                } else {
                    result.affectedRows = statement.getUpdateCount();
                    
                    ResultSet generatedKeysResultSet = statement.getGeneratedKeys();
                    if (generatedKeysResultSet.next())
                        result.insertId = generatedKeysResultSet.getInt(1);
                }
                
                query.result = result;
                
            } catch (SQLException exception) {
                String message = "Error while executing the MySQL query (" + 
                        exception.getErrorCode() + "): " + exception.getMessage();

                query.error = message;
            }
            
            return query;
        }
        
        // Connects to the database, and returns whether the connection was successful. This method
        // will output error messages to the logger if it couldn't establish a new connection.
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
        
        // Closes the established connection with the database, if it's still active. This normally
        // happens when the database thread is being terminated.
        private void disconnect() {
            if (mConnection == null)
                return;
            
            try {
                mConnection.close();
            } catch (SQLException e) { /** It's safe to ignore this exception **/ }

            mLogger.info("Mineground has closed the connection with the database!");
            mConnection = null;
        }
        
        // Requests a shutdown of the database thread. After all currently queued queries have
        // finished, this thread will automatically exit.
        public void requestShutdown() {
            mShutdownRequested = true;
        }
        
        // Enqueues |pendingQuery| to be executed on the database thread.
        public void enqueue(PendingQuery pendingQuery) {
            mPendingQueryQueue.add(pendingQuery);
        }
        
        // Immediately returns the PendingQuery object of a finished query if one is available. The
        // name emphasizes the fact that we will not block the main thread on this.
        public PendingQuery immediatelyRetrieveFinishedQuery() {
            return mFinishedQueryQueue.poll();
        }
    }
    
    // Instance of the thread which will be used for this connection.
    private final DatabaseThread mDatabaseThread;
    
    public DatabaseConnectionImpl(DatabaseConnectionParams params) {
        mLogger = Logger.getLogger("DatabaseConnection");
        mDatabaseThread = new DatabaseThread(params);
    }
    
    // Starts the database thread, which will then start its attempts in establishing a connection
    // with the MySQL information, using the DatabaseConnectionParams provided.
    public void connect() {
        mDatabaseThread.start();
    }

    // Disconnects from the database by requesting the database thread to terminate. If it doesn't
    // terminate within five seconds, we will consider the thread as being lost.
    public void disconnect() {
        mDatabaseThread.requestShutdown();
        try {
            mDatabaseThread.join(MAXIMUM_DISCONNECT_WAIT_TIME);
        } catch (InterruptedException exception) {
            mLogger.severe("Database shutdown has been interrupted: " + exception.getMessage());
        }
    }

    // Creates a PendingQuery instance holding |query|, and adds it to a queue on the database
    // thread. The promise belonging to the PendingQuery will be returned.
    public Promise<DatabaseResult> enqueueQueryForExecution(String query) {
        if (mDatabaseThread == null)
            throw new RuntimeException("A query is being queued for execution while the database thread is inactive.");
        
        PendingQuery pendingQuery = new PendingQuery(query);
        mDatabaseThread.enqueue(pendingQuery);

        return pendingQuery.promise;
    }

    // Reads all finished PendingQuery instance from the database thread and settles their promises
    // based on what result information is available on them.
    public void doPollForResults() {
        PendingQuery finishedQuery = mDatabaseThread.immediatelyRetrieveFinishedQuery();
        while (finishedQuery != null) {
            if (finishedQuery.result != null)
                finishedQuery.promise.resolve(finishedQuery.result);
            else
                finishedQuery.promise.reject(new PromiseError(finishedQuery.error));
            
            finishedQuery = mDatabaseThread.immediatelyRetrieveFinishedQuery();
        }
    }
}
