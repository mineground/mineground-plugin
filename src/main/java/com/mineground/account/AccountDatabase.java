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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import com.mineground.base.Promise;
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
import com.mineground.database.Database;
import com.mineground.database.DatabaseResult;
import com.mineground.database.DatabaseResultRow;
import com.mineground.database.DatabaseStatement;

// Whereas the AccountManager class curates the account, this class is responsible for loading,
// storing and creating accounts.
public class AccountDatabase {
    // Logger used for sharing database errors with the console in case they happen.
    private final Logger mLogger = Logger.getLogger(AccountDatabase.class.getCanonicalName());

    // Date format which we use for reading and storing dates in the database.
    private final DateFormat mDateFormat;
    
    // The following statements are the queries which are being used for loading, creating and
    // updating accounts in the database. See the constructor for more detailed documentation.
    private final DatabaseStatement mLoadAccountStatement;
    private final DatabaseStatement mCreateUserStatement;
    private final DatabaseStatement mCreateUserSettingsStatement;
    private final DatabaseStatement mUpdateUserStatement;
    private final DatabaseStatement mUpdateUserSettingsStatement;
    
    public AccountDatabase(Database database) {
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Statement used for loading accounts from the database. The player's name will be used
        // as the key to identify them from.
        mLoadAccountStatement = database.prepare(
                "SELECT " +
                    "users.user_id, " +
                    "users.username, " +
                    "users.password, " +
                    "users.level, " +
                    "users.registered, " +
                    "users_settings.online_time, " +
                    "users_settings.kill_count, " +
                    "users_settings.death_count, " +
                    "users_settings.stats_reaction, " +
                    "users_settings.stats_blocks_created, " +
                    "users_settings.stats_blocks_destroyed, " +
                    "users_settings.last_seen " +
                "FROM " +
                    "users " +
                "LEFT JOIN " +
                    "users_settings ON users_settings.user_id = users.user_id " +
                "WHERE " +
                    "users.username = ?"
        );
        
        // Statement used for creating a new entry for this user in the database. After this
        // statement successfully executes, an entry in the users_settings table will be created.
        mCreateUserStatement = database.prepare(
                "INSERT INTO " +
                    "users " +
                    "(username, registered) " +
                "VALUES " +
                    "(?, NOW())"
        );
        
        // Statement for creating a new entry in the users_settings table in the database. This
        // row will contain statistics and more generic information about the player.
        mCreateUserSettingsStatement = database.prepare(
                "INSERT INTO " +
                    "users_settings " +
                    "(user_id, last_ip, last_seen) " +
                "VALUES " +
                    "(?, INET_ATON(?), NOW())"
        );
        
        // Statement to update the users table with the latest unique_id and password.
        mUpdateUserStatement = database.prepare(
                "UPDATE " +
                    "users " +
                "SET " +
                    "password = ? " +
                "WHERE " +
                    "user_id = ?"
        );
        
        // Statement to update the user's settings with the latest information.
        mUpdateUserSettingsStatement = database.prepare(
                "UPDATE " +
                    "users_settings " +
                "SET " +
                    "online_time = ?, " +
                    "kill_count = ?, " +
                    "death_count = ?, " +
                    "stats_reaction = ?, " +
                    "stats_blocks_created = ?, " +
                    "stats_blocks_destroyed = ?, " +
                    "last_seen = ? " +
                "WHERE " +
                    "user_id = ?"
        );
    }
    
    // Loads the account of |player| from the database. If it does not exist yet, a new account will
    // be created, which allows them to play as a guest.
    public Promise<AccountData> loadOrCreateAccount(final Player player) {
        final Promise<AccountData> promise = new Promise<AccountData>();
        
        mLoadAccountStatement.setString(1, player.getName());
        mLoadAccountStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                if (result.rows.size() == 0) {
                    if (player.isOnline())
                        createAccount(player, promise);

                    return;
                }
                
                final DatabaseResultRow resultRow = result.rows.get(0);
                final AccountData accountData = new AccountData(player);
                
                // Table: users
                accountData.user_id = resultRow.getInteger("user_id").intValue();
                accountData.username = resultRow.getString("username");
                accountData.password = resultRow.getString("password");
                accountData.level = AccountLevel.fromString(resultRow.getString("level"));
                try {
                    accountData.registered = mDateFormat.parse(resultRow.getString("registered"));
                } catch (ParseException exception) { /** registered will default to NOW **/ }
                
                // Table: users_settings
                accountData.online_time = resultRow.getInteger("online_time").intValue();
                accountData.kill_count = resultRow.getInteger("kill_count").intValue();
                accountData.death_count = resultRow.getInteger("death_count").intValue();
                accountData.stats_reaction = resultRow.getInteger("stats_reaction").intValue();
                accountData.stats_blocks_created = resultRow.getInteger("stats_blocks_created").intValue();
                accountData.stats_blocks_destroyed = resultRow.getInteger("stats_blocks_destroyed").intValue();
                try {
                    accountData.last_seen = mDateFormat.parse(resultRow.getString("last_seen"));
                } catch (ParseException exception) { }
                
                // Now that the AccountData object is complete, resolve the promise to let the
                // account manager know that this user's information is available.
                promise.resolve(accountData);
            }

            public void onRejected(PromiseError error) {
                mLogger.severe("Unable to load the account of " + player.getName() + ".");
                mLogger.severe(error.reason());

                promise.reject("An error occurred when loading the account from database.");
            }
        });
        
        return promise;
    }
    
    // Creates an account for |player|, and resolves |promise| with a valid AccountData instance
    // once that has succeeded. This will implicitly register the player as a guest.
    public void createAccount(final Player player, final Promise<AccountData> promise) {
        mCreateUserStatement.setString(1, player.getName());
        mCreateUserStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                final AccountData accountData = new AccountData(player);
                accountData.user_id = result.insertId;
                accountData.username = player.getName();
                
                mCreateUserSettingsStatement.setInteger(1, result.insertId);
                mCreateUserSettingsStatement.setString(2, player.getAddress().getAddress().getHostAddress());
                mCreateUserSettingsStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
                    public void onFulfilled(DatabaseResult result) { /** Yippie! **/ }
                    public void onRejected(PromiseError error) {
                        mLogger.severe("Unable to create new account settings for " + player.getName() + ".");
                        mLogger.severe(error.reason());
                    }
                });
                
                // There is no need to wait for the player's row to be available in users_settings,
                // so resolve the promise immediately with the newly created AccountData object.
                promise.resolve(accountData);
            }

            public void onRejected(PromiseError error) {
                mLogger.severe("Unable to create a new account for " + player.getName() + ".");
                mLogger.severe(error.reason());
                
                promise.reject("An error occurred when creating a new account in the database.");
            }
        });
    }
    
    // Updates the database with the mutable fields in the AccountData instance |accountData|. 
    public void updateAccount(final AccountData accountData) {
        mUpdateUserStatement.setString(1, accountData.password);
        mUpdateUserStatement.setInteger(2, accountData.user_id);
        mUpdateUserStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) { /** Yippie! **/ }
            public void onRejected(PromiseError error) {
                mLogger.severe("Unable to update user information for " + accountData.username + ".");
                mLogger.severe(error.reason());
            }
        });
        
        mUpdateUserSettingsStatement.setInteger(1, accountData.online_time);
        mUpdateUserSettingsStatement.setInteger(2, accountData.kill_count);
        mUpdateUserSettingsStatement.setInteger(3, accountData.death_count);
        mUpdateUserSettingsStatement.setInteger(4, accountData.stats_reaction);
        mUpdateUserSettingsStatement.setInteger(5, accountData.stats_blocks_created);
        mUpdateUserSettingsStatement.setInteger(6, accountData.stats_blocks_destroyed);
        mUpdateUserSettingsStatement.setString(7, mDateFormat.format(new Date()));
        mUpdateUserSettingsStatement.setInteger(8, accountData.user_id);
        mUpdateUserSettingsStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) { /** Yippie! **/ }
            public void onRejected(PromiseError error) {
                mLogger.severe("Unable to update user settings for " + accountData.username + ".");
                mLogger.severe(error.reason());
            }
        });
    }
}
