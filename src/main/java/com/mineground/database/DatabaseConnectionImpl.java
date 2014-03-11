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

import java.util.logging.Logger;

import com.mineground.base.Promise;
import com.mineground.base.PromiseError;

/**
 * Implementation of the DatabaseConnection interface, based on a JDBC connection using the MySQL
 * J/Connection connector. A thread is used for asynchronous communication with the database.
 */
public class DatabaseConnectionImpl implements DatabaseConnection {
    /**
     * The maximum number of *milliseconds* which the main thread will be waiting on the database
     * thread to shutdown cleanly. When this expires, the database thread will be considered dead.
     */
    private final static int MAXIMUM_DISCONNECT_WAIT_TIME = 5000;
    
    /**
     * Logger used for outputting warnings and errors occurring on the database connection.
     */
    private final Logger mLogger;
    
    /**
     * Instance of the thread which will be used for this connection.
     */
    private final DatabaseThread mDatabaseThread;
    
    public DatabaseConnectionImpl(DatabaseConnectionParams params) {
        mLogger = Logger.getLogger("DatabaseConnection");
        mDatabaseThread = new DatabaseThread(params);
    }
    
    /**
     * Starts the database thread, which will then start its attempts in establishing a connection
     * with the MySQL information, using the DatabaseConnectionParams provided.
     */
    public void connect() {
        mDatabaseThread.start();
    }

    /**
     * Disconnects from the database by requesting the database thread to terminate. If it doesn't
     * terminate within five seconds, we will consider the thread as being lost.
     */
    public void disconnect() {
        mDatabaseThread.requestShutdown();
        try {
            mDatabaseThread.join(MAXIMUM_DISCONNECT_WAIT_TIME);
        } catch (InterruptedException exception) {
            mLogger.severe("Database shutdown has been interrupted: " + exception.getMessage());
            mLogger.severe("This means that user data may have been lost due to unexecuted queries!");
        }
    }

    /**
     * Creates a PendingQuery instance holding |query|, and adds it to a queue on the database
     * thread. The promise belonging to the PendingQuery will be returned.
     */
    public Promise<DatabaseResult> enqueueQueryForExecution(String query, DatabaseStatementParams parameters) {
        if (mDatabaseThread == null)
            throw new RuntimeException("A query is being queued for execution while the database thread is inactive.");
        
        PendingQuery pendingQuery = new PendingQuery(query, parameters);
        mDatabaseThread.enqueue(pendingQuery);

        return pendingQuery.promise;
    }

    /**
     * Reads all finished PendingQuery instance from the database thread and settles their promises
     * based on what result information is available on them.
     */
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
