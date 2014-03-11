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

package com.mineground.account;

import com.mineground.database.Database;
import com.mineground.database.DatabaseStatement;

/**
 * The player log allows Mineground to keep track of the actions of various players. Our primary
 * interest in doing this is to make sure that when players abuse certain functionality, we have the
 * ability to find who did it, and when. Features are encouraged to log any modification.
 */
public class PlayerLog {
    /**
     * The following types of records may be used throughout Mineground. When adding a new record
     * type, add a new row in the "record_types" database table. The inserted Id is the number to
     * place after the enumeration entry.
     * 
     * DO NOT RE-USE RECORD TYPE IDs. That can lead to seriously skewed data because older entries
     * with the same Id may will be around.
     */
    public enum RecordType {
        // -----------------------------------------------------------------------------------------
        // Group: Connectivity, measuring when players connect to the server. 
        // -----------------------------------------------------------------------------------------

        CONNECTED(1),           // Records when a player successfully connects to Mineground.
        DISCONNECTED(2),        // Records when a player closes the connection with Mineground.
        
        // -----------------------------------------------------------------------------------------
        // Group: Features, measuring usage of certain features.
        // -----------------------------------------------------------------------------------------
        
        WARP_CREATED(3),        // Records when a player saves a location using /warp create.
        WARP_TELEPORTED(4),     // Records when a player teleports to another location using /warp.
        WARP_REMOVED(5),        // Records when a player has removed a saved location.
        
        // -----------------------------------------------------------------------------------------
        
        INVALID(-1);
        
        private int database_record_type_id;
        private RecordType(int type_id) {
            database_record_type_id = type_id;
        }
    }
    
    /**
     * The database statement which will be used for writing a new record to the database. Each new
     * record will be written immediately, we can gain reasonably big performance improvements here
     * by grouping writes together, but the server is not yet busy enough for that.
     */
    private static DatabaseStatement sWriteRecordStatement;
    
    /**
     * Either initializes or finalizes the PlayerLog depending on the value of |database|. If it's
     * null, then nullify the write record statement we staticly keep. Otherwise create a new write
     * record statement, as there seems to be a new database connection.
     * 
     * @param database  The active database connection for Mineground.
     */
    public static void setDatabase(Database database) {
        if (database == null) {
            sWriteRecordStatement = null;
            return;
        }
        
        sWriteRecordStatement = database.prepare(
                "INSERT INTO " +
                    "records (record_type_id, player_id, extra_int, extra_text) " +
                "VALUES " +
                    "(?, ?, ?, ?)"
        );
    }

    /**
     * Writes a new record of type |type| to the database, owned by |user_id|.
     * 
     * @param type          Type of record to write to the database.
     * @param user_id       Id of the user who was responsible for creating this record.
     * @param extra_int     Extra integer parameter to store with the record.
     * @param extra_text    Extra textual parameter to store with the record.
     */
    public static void record(RecordType type, int user_id, int extra_int, String extra_text) {
        if (sWriteRecordStatement == null)
            return;

        sWriteRecordStatement.setInteger(1, type.database_record_type_id);
        sWriteRecordStatement.setInteger(2, user_id);
        sWriteRecordStatement.setInteger(3, extra_int);
        sWriteRecordStatement.setString(4, extra_text);
        sWriteRecordStatement.execute();
    }
    
    /**
     * Writes a new record of type |type| to the database, owned by |user_id|.
     * 
     * @param type          Type of record to write to the database.
     * @param user_id       Id of the user who was responsible for creating this record.
     * @param extra_text    Extra textual parameter to store with the record.
     */
    public static void record(RecordType type, int user_id, String extra_text) {
        record(type, user_id, 0, extra_text);
    }
    
    /**
     * Writes a new record of type |type| to the database, owned by |user_id|.
     * 
     * @param type      Type of record to write to the database.
     * @param user_id   Id of the user who was responsible for creating this record.
     * @param extra_int Extra integer parameter to store with the record.
     */
    public static void record(RecordType type, int user_id, int extra_int) {
        record(type, user_id, extra_int, "");
    }

    /**
     * Writes a new record of type |type| to the database, owned by |user_id|.
     * 
     * @param type      Type of record to write to the database.
     * @param user_id   Id of the user who was responsible for creating this record.
     */
    public static void record(RecordType type, int user_id) {
        record(type, user_id, 0, "");
    }
}
