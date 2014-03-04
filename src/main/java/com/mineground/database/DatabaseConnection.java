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

// The database connection class curates the actual connection with the database, and owns the
// execution thread on which queries will be executed. This interface defines the API with which
// the Database class can communicate with it, keeping implementation details separate.
public interface DatabaseConnection {
    // Asynchronously establishes a connection with the database. If the connection is lost at any
    // point during the plugin's lifetime, the implementation will make a best effort to reestablish
    // it. The implementation will output diagnostic information to a logger.
    public void connect();
    
    // Synchronously disconnects from the database and returns when the connection is dead. When
    // the implementation has gone unresponsive, it will return after a 5 second timeout as well.
    public void disconnect();
    
    // Enqueues |query| to be asynchronously executed on the database. A promise will be returned,
    // which will be settled when a result has been made available. The |parameters| argument may
    // be used to supply a map of parameters which should be securely replaced in the query.
    public Promise<DatabaseResult> enqueueQueryForExecution(String query, DatabaseStatementParams parameters);
    
    // Polls for finished database queries from the database thread, for which the promises can be
    // settled. This method should be called every 2 server ticks (~100ms) on the main thread.
    public void doPollForResults();
}
