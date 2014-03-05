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
// lifetime. This class is only responsible for gathering the data required to create a prepared
// statement in the database connection implementation.
public class DatabaseStatement {
    // The Database which this statement has been created for, and the |execute()| method will defer
    // to for execution of the statement itself.
    private Database mDatabase;
    
    // The query which this statement will prepare. Each time the character "?" (question mark) is
    // found, it will be replaced with one of the parameters to this statement.
    private String mQuery;
    
    // Map of parameters which should be set in the query. All entries will be stored as Objects,
    // which will be converted back to their values in the database connection implementation.
    private DatabaseStatementParams mParameters;
    
    public DatabaseStatement(Database database, String query) {
        mParameters = new DatabaseStatementParams();
        mDatabase = database;
        mQuery = query;
    }
    
    // Sets parameter |parameterIndex| in |mQuery| to equal the string |value|.
    public DatabaseStatement setString(int parameterIndex, String value) {
        mParameters.put(parameterIndex, value);
        return this;
    }
    
    // Sets parameter |parameterIndex| in |mQuery| to equal the integer |value|.
    public DatabaseStatement setInteger(int parameterIndex, Long value) {
        mParameters.put(parameterIndex, value);
        return this;
    }
    
    // Sets parameter |parameterIndex| in |mQuery| to equal the double |value|.
    public DatabaseStatement setDouble(int parameterIndex, Double value) {
        mParameters.put(parameterIndex, value);
        return this;
    }
    
    // Sends |mQuery| to the database connection to be executed, together with the parameters as
    // they have been stored for this statement. Preparing the statement will be done by the thread.
    public Promise<DatabaseResult> execute() {
        return mDatabase.query(mQuery, mParameters);
    }
}
