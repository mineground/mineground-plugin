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

import com.mineground.base.Promise;
import com.mineground.base.PromiseError;

// The database connection class curates the actual connection with the database, and owns the
// execution thread on which queries will be executed.
public class DatabaseConnection {
    // TODO: Implement support for query timeouts.
    
    // Connection parameters which are being used to connect to the database. This field will be
    // assigned to once (in the constructor) and should be considered immutable thereafter.
    private final DatabaseConnectionParams mConnectionParams;
    
    public DatabaseConnection(DatabaseConnectionParams connectionParams) {
        mConnectionParams = connectionParams;
    }
    
    // Synchronously establishes a connection with the database, and returns whether that has
    // succeeded. In case of a failure, log messages will be outputted to the console.
    public boolean connect() {
        // TODO: Synchronously connect to the database and start the thread.
        return false;
    }
    
    // Synchronously disconnects from the database and returns when both the connection is dead, and
    // the database thread has been terminated as well.
    public void disconnect() {
        // TODO: Synchronously disconnect from the database and stop the thread.
    }

    // Enqueues |query| to be executed on the database thread. A promise will be returned which will
    // be executed when a result has been made available.
    public Promise<DatabaseResult> enqueueQueryForExecution(String query) {
        Promise<DatabaseResult> promise = new Promise<DatabaseResult>();
        promise.reject(new PromiseError("The database thread has not been implemented yet."));
        
        return promise;
    }
    
    // Polls for finished database queries from the database thread, for which the promises can be
    // settled. This method will be called every 2 server ticks (~100ms) on the main thread.
    public void doPollForResults() {
        // TODO: Poll for results in here.
    }
}
