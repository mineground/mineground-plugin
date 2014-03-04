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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.mineground.base.Promise;
import com.mineground.base.PromiseError;

// The Database class is the public-facing API for database communications within Mineground. All
// queries to be executed on this database will be ran asynchronously.
public class Database {
    // Id of an invalid task in the Bukkit scheduler. We can safely pass this value to Bukkit as
    // a task Id regardless of whether its ours or not, so use it as the default value.
    private static final int INVALID_TASK_ID = -1;

    // Configuration (mineground.yml) based on which the connection will be established.
    private FileConfiguration mConfiguration;
    
    // Connection API through which all Database operations will communicate with the database.
    // The connection itself will execute all queries on a separate thread.
    private DatabaseConnection mConnection;
    
    private JavaPlugin mPlugin;
    
    // Task Id of the running repeating task within Bukkit's task scheduler. When the database is
    // disconnected, the task needs to be unregistered as well.
    private int mSchedulerTaskId;
    
    public Database(FileConfiguration configuration, JavaPlugin plugin) {
        mConfiguration = configuration;
        mPlugin = plugin;
        
        mSchedulerTaskId = INVALID_TASK_ID;
    }
    
    // Starts the Database Thread, which will attempt to connect to the database. The thread will
    // automatically reconnect if the connection is lost at any point during the plugin's lifetime.
    public void connect() {
        DatabaseConnectionParams params = new DatabaseConnectionParams();
        params.hostname = mConfiguration.getString("database.hostname", "localhost");
        params.port = mConfiguration.getInt("database.port", 3306);
        params.username = mConfiguration.getString("database.username", "changeme");
        params.password = mConfiguration.getString("database.password", "");
        params.database = mConfiguration.getString("database.database", "mineground");
        
        mConnection = new DatabaseConnection(params);
        mConnection.connect();
        
        // Register a task with the Bukkit Scheduler to poll for results every 2 server ticks. The
        // Bukkit server has 20 ticks per second (once per 50ms), so the expected maximum delay in
        // relaying database results is about a hundred milliseconds.
        mSchedulerTaskId = getScheduler().scheduleSyncRepeatingTask(mPlugin, new Runnable() {
            public void run() {
                if (mConnection != null)
                    mConnection.doPollForResults();
            }
        }, 2, 2);
    }
    
    // Synchronously stops the database thread after all pending queries have been executed, and
    // disconnects the established connection with the database.
    public void disconnect() {
        if (mConnection == null)
            return;

        mConnection.disconnect();
        mConnection = null;

        getScheduler().cancelTask(mSchedulerTaskId);
        mSchedulerTaskId = INVALID_TASK_ID;
    }
    
    // Prepares |query| as a database statement, making it more convenient and safer to perform
    // operations on it when the query will be used in the future.
    public DatabaseStatement prepare(String query) {
        return new DatabaseStatement(this, query);
    }
    
    // Executes |query| on the database and returns a promise which will be settled depending on the
    // result. If the query succeeds, the promise will be resolved with a DatabaseResult instance,
    // otherwise the promise will be rejected sharing the error which occurred in the database.
    public Promise<DatabaseResult> query(String query) {
        if (mConnection != null)
            return mConnection.enqueueQueryForExecution(query);
        
        Promise<DatabaseResult> promise = new Promise<DatabaseResult>();
        promise.reject(new PromiseError("Mineground does not have an established connection with the database."));
        return promise;
    }
    
    // Returns the Bukkit scheduler from |mPlugin|.
    private BukkitScheduler getScheduler() { return mPlugin.getServer().getScheduler(); }
}
