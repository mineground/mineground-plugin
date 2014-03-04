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

// The DatabaseStatement class encapsulates a prepared statement which can be reused during its
// lifetime. Whilst this class does not represent a formal implementation of prepared statements,
// the current aim is to make database communications in Mineground as convenient as possible.
public class DatabaseStatement {
    // The Database will be used to execute this statement on as a normal query.
    private Database mDatabase;
    
    // The query which this statement will prepare. Each time the character "?" (question mark) is
    // found, it will be replaced with one of the parameters to this statement.
    private String mQuery;
    
    public DatabaseStatement(Database database, String query) {
        mDatabase = database;
        mQuery = query;
    }
    
    // TODO: Recognize the number of required parameters in the query.
    // TODO: Implement setters for the individual parameters in the query.
    
    // Executes |mQuery| with all parameters replaced with the values as set on the statement.
    public Promise<DatabaseResult> execute() {
        String preparedQuery = mQuery;
        
        // TODO: Prepare |preparedQuery| using the available parameters.
        
        return mDatabase.query(preparedQuery);
    }
}