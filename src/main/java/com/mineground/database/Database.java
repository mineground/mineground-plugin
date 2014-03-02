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

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitScheduler;

import com.mineground.base.Promise;

// The Database class is the public-facing API for database communications within Mineground. All
// queries to be executed on this database will be ran asynchronously.
public class Database {
    // Configuration (mineground.yml) based on which the connection will be established.
    private FileConfiguration mConfiguration;
    
    // Connection API through which all Database operations will communicate with the database.
    // The connection itself will execute all queries on a separate thread.
    private DatabaseConnection mConnection;
    
    // The Bukkit Scheduler is used to execute a poll for finished database results from the
    // database thread, because we want to deliver the results on the main thread.
    private BukkitScheduler mBukkitScheduler;
    
    public Database(FileConfiguration configuration, BukkitScheduler scheduler) {
        mConfiguration = configuration;
        mBukkitScheduler = scheduler;
    }
    
    // Synchronously connects to the database and returns whether the connection was established
    // successfully. The Database system will automatically reconnect if the connection is lost at
    // any point during the plugin's lifetime.
    public boolean connect() {
        // TODO: Initialize the database connection and start the thread.
        return false;
    }
    
    // Synchronously stops the database thread after all pending queries have been executed, and
    // disconnects the established connection with the database.
    public void disconnect() {
        // TODO: Close the database connection and stop the thread.
    }
    
    // Prepares |query| as a database statement, making it more convenient and safer to perform
    // operations on it when the query will be used in the future.
    public DatabaseStatement prepare(String query) {
        return new DatabaseStatement(mConnection, query);
    }
    
    // Executes |query| on the database and returns a promise which will be settled depending on the
    // result. If the query succeeds, the promise will be resolved with a DatabaseResult instance,
    // otherwise the promise will be rejected sharing the error which occurred in the database.
    public Promise<DatabaseResult> query(String query) {
        // TODO: Implement support for queries.
        return null;
    }
}
