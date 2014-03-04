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

// Implementation of the DatabaseConnection interface, based on a JDBC connection using the MySQL
// J/Connection connector. A thread is used for asynchronous communication with the database.
public class DatabaseConnectionImpl implements DatabaseConnection {
    
    // Logger used for outputting warnings and errors occurring on the database connection. The
    // instance will be used from both the main and database threads, and is guaranteed to be safe.
    private final Logger mLogger;
    
    // The DatabaseThread is the thread which actually communicates with the MySQL database. We run
    // this on a separate thread since queries should not block the rest of the server.
    private class DatabaseThread extends Thread {
        // Connection parameters which are being used to connect to the database. This field will be
        // assigned to once (in the constructor) and should be considered immutable thereafter.
        private final DatabaseConnectionParams mConnectionParams;
        
        public DatabaseThread(DatabaseConnectionParams connectionParams) {
            mConnectionParams = connectionParams;
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
        // TODO: Implement this method.
    }

    // Disconnects from the database by requesting the database thread to terminate. If it doesn't
    // terminate within five seconds, we will consider the thread as being lost.
    public void disconnect() {
        // TODO: Implement this method.
    }

    // Creates a PendingQuery instance holding |query|, and adds it to a queue on the database
    // thread. The promise belonging to the PendingQuery will be returned.
    public Promise<DatabaseResult> enqueueQueryForExecution(String query) {
        // TODO: Implement this method.
        return null;
    }

    // Reads all finished PendingQuery instance from the database thread and settles their promises
    // based on what result information is available on them.
    public void doPollForResults() {
        // TODO: Implement this method.
    }
}
