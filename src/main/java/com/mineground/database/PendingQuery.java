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

// Class for representing a query which has not been executed yet, or which has been executed
// but whose promise has not been settled yet.
public class PendingQuery {
    public Promise<DatabaseResult> promise;
    
    // In: The query which should be executed on the database, and an optional map of parameters
    // which should be replaced in the query, when it's treated as a prepared statement.
    public String query;
    public DatabaseStatementParams parameters;
    
    // Out: The DatabaseResult object if available, or a String containing the error message.
    public DatabaseResult result;
    public String error;
    
    public PendingQuery(String query_, DatabaseStatementParams parameters_) {
        promise = new Promise<DatabaseResult>();
        query = query_;
        parameters = parameters_;
    }
}