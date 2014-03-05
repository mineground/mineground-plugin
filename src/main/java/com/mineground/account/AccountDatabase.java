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

import org.bukkit.entity.Player;

import com.mineground.base.Promise;
import com.mineground.database.Database;

// Whereas the AccountManager class curates the account, this class is responsible for loading,
// storing and creating accounts.
public class AccountDatabase {
    // Instance of the database connection using which account information can be accessed.
    private final Database mDatabase;

    public AccountDatabase(Database database) {
        mDatabase = database;
    }
    
    public Promise<AccountData> loadOrCreateAccount(final Player player) {
        return null;
    }
    
    public void updateAccount(final Player player, AccountData account) {

    }
}
