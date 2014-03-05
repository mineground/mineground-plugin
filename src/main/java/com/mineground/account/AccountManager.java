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

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import com.mineground.EventDispatcher;
import com.mineground.database.Database;

// The account manager curates the player accounts on Mineground, their statistics, information,
// economics and everything else related to it. Each player implicitly gets an account, however,
// without having registered their account on the website it will be considered as a guest account.
public class AccountManager {
    // Interface between the account manager and the database.
    private final AccountDatabase mAccountDatabase;
    
    // Map between Bukkit players and their Mineground accounts.
    private final Map<Player, Account> mPlayerAccountMap;
    
    public AccountManager(Database database) {
        mAccountDatabase = new AccountDatabase(database);
        mPlayerAccountMap = new HashMap<Player, Account>();
    }
    
    // Start preloading the account of a user whose name is |nickname|. The user won't be considered
    // connected yet, since other plugins may take the honors of denying them access.
    public void preloadAccount(String nickname) {
        // TODO: Start preloading the account.
    }
    
    // Loads the account, and don't fire the onPlayerJoined event on the dispatcher until their
    // information has been loaded and verified. This will be called for all online players when the
    // module is being loaded while there already are players in-game.
    public void loadAccount(final Player player, final EventDispatcher dispatcher) {
        // TODO: Load the account. Only call onPlayerJoined() when it has succeeded.
        dispatcher.onPlayerJoined(player);
    }
    
    // Retrieves the account for |player|. If no account is available for them, NULL will be
    // returned instead. That just means that no information has been loaded yet.
    public Account getAccountForPlayer(Player player) {
        return mPlayerAccountMap.get(player);
    }
}
