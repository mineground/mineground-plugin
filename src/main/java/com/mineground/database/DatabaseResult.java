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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The DatabaseResult class encapsulates a single result set from the database. It contains relevant
// information for SELECT queries (the selected rows), as well as for UPDATE and DELETE queries
// (the number of affected rows) and INSERT queries (the newly inserted primary key Id). Most of the
// members of this class are publicly exposed, any may be accessed directly.
public class DatabaseResult {
    // Value which will be returned by columnNameToIndex if the column cannot be found.
    public final static int INVALID_COLUMN_INDEX = -1;
    
    // A mapping between column names and column indices, allowing more convenient access to columns
    // in the retrieved information by being able to refer to them by their name.
    private final Map<String, Integer> mColumnNameToIndexMap;
    
    // The number of rows which were affected by the UPDATE or DELETE operation.
    public int affectedRows;
    
    // The inserted primary key Id for the INSERT operation.
    public int insertId;
    
    // The rows returned from the database containing the fetched information for SELECT operations.
    public final List<DatabaseResultRow> rows;
    
    public DatabaseResult() {
        mColumnNameToIndexMap = new HashMap<String, Integer>();
        rows = new ArrayList<DatabaseResultRow>();
        affectedRows = 0;
        insertId = 0;
    }
    
    // Sets the column names based on which the columnNameToIndex() method will do its magic. This
    // method should only be called from the database connection implementation.
    public void setColumnNames(List<String> columnNames) {
        int columnIndex = 1;
        for (String columnName : columnNames)
            mColumnNameToIndexMap.put(columnName, columnIndex++);
    }
    
    // Returns the column index for |columnName| in the result set. If the column does not exist,
    // the INVALID_COLUMN_INDEX constant will be returned instead.
    public int columnNameToIndex(String columnName) {
        final Integer columnIndex = mColumnNameToIndexMap.get(columnName);
        if (columnIndex == null)
            return INVALID_COLUMN_INDEX;
        
        return columnIndex;
    }
}
