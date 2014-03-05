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
import java.util.List;

// Represents an individual row in a database result set. Fields may be retrieved from the set by
// using the getString, getInteger or getDouble getters, each of which may be passed in a column
// index, or a column name.
public class DatabaseResultRow {
    // The result which this row belongs to, used for retrieving the column index from its name.
    private final DatabaseResult mResult;
    
    // List of the fields in this row. They can be retrieved using the various getters.
    private final List<Object> mFieldList;
    
    public DatabaseResultRow(DatabaseResult result, int columnCount) {
        mFieldList = new ArrayList<Object>(columnCount);
        mResult = result;
    }
    
    // Retrieves the value for field |columnIndex| as a String. Mind that column indices in this
    // database system are one-based, to match common conventions in Java.
    public String getString(int columnIndex) {
        --columnIndex; // map one-based indices to zero-based ones.
        if (columnIndex < 0 || columnIndex >= mFieldList.size())
            return null;
        
        return (String) mFieldList.get(columnIndex);
    }
    
    // Retrieves the value for field |columnName| as a String.
    public String getString(String columnName) {
        int columnIndex = mResult.columnNameToIndex(columnName);
        if (columnIndex == DatabaseResult.INVALID_COLUMN_INDEX)
            return null;
        
        return getString(columnIndex);
    }
    
    // Retrieves the value for field |columnIndex| as an integer. Mind that column indices in this
    // database system are one-based, to match common conventions in Java.
    public Long getInteger(int columnIndex) {
        --columnIndex; // map one-based indices to zero-based ones.
        if (columnIndex < 0 || columnIndex >= mFieldList.size())
            return null;
        
        return (Long) mFieldList.get(columnIndex);
    }
    
    // Retrieves the value for field |columnName| as an integer.
    public Long getInteger(String columnName) {
        int columnIndex = mResult.columnNameToIndex(columnName);
        if (columnIndex == DatabaseResult.INVALID_COLUMN_INDEX)
            return null;
        
        return getInteger(columnIndex);
    }
    
    // Retrieves the value for field |columnIndex| as a double. Mind that column indices in this
    // database system are one-based, to match common conventions in Java.
    public Double getDouble(int columnIndex) {
        --columnIndex; // map one-based indices to zero-based ones.
        if (columnIndex < 0 || columnIndex >= mFieldList.size())
            return null;
        
        return (Double) mFieldList.get(columnIndex);
    }
    
    // Retrieves the value for field |columnName| as a double.
    public Double getDouble(String columnName) {
        int columnIndex = mResult.columnNameToIndex(columnName);
        if (columnIndex == DatabaseResult.INVALID_COLUMN_INDEX)
            return null;
        
        return getDouble(columnIndex);
    }
    
    // Pushes |value| on the list of fields belonging to this row. Whilst type information is of
    // course available in Java's Object object, we don't store an explicit notion of it.
    public void add(Object value) {
        mFieldList.add(value);
    }
}
