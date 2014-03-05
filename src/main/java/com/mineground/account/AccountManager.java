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
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
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
    
    // Loads the account, and don't fire the onPlayerJoined event on the dispatcher until their
    // information has been loaded and verified. This will be called for all online players when the
    // plugin is being loaded while there already are players in-game.
    public void loadAccount(final Player player, final EventDispatcher dispatcher) {
        mPlayerAccountMap.put(player,  new Account());
        mAccountDatabase.loadOrCreateAccount(player).then(new PromiseResultHandler<AccountData>() {
            public void onFulfilled(AccountData accountData) {
                if (!player.isOnline())
                    return;

                // Try to authenticate the player with their account information. We will not
                // authenticate the player with their account until we know it's really them.
                authenticatePlayerAccount(player, accountData, dispatcher);
            }

            // Something went wrong when trying to create an account for this player.
            public void onRejected(PromiseError error) {
                if (!player.isOnline())
                    return;
                
                player.sendMessage("Your account could not be loaded, so we've logged you in as a guest.");
                player.sendMessage("Please contact an administrator to get this resolved!");
                dispatcher.onPlayerJoined(player);
            }
        });
    }
    
    // Authenticates |player| based on their |accountData|. Mineground support silent log in for
    // players with an official client, whereas other players will have to enter their password.
    private void authenticatePlayerAccount(final Player player, final AccountData accountData, final EventDispatcher dispatcher) {
        // TODO: Authenticate the player.
        
        final Account account = mPlayerAccountMap.get(player);
        if (account == null)
            return;
        
        account.load(accountData);
        dispatcher.onPlayerJoined(player);
    }
    
    // Called when the player is leaving the server, meaning we should store the latest updates to
    // their account in the database. When the Mineground plugin is disabled, this method will be
    // called for all players to ensure that we properly store all information.
    public void unloadAccount(Player player) {
        final Account account = mPlayerAccountMap.get(player);
        if (account == null)
            return;
        
        mAccountDatabase.updateAccount(account.getAccountData());
        mPlayerAccountMap.remove(player);
    }
    
    // Retrieves the account for |player|. If no account is available for them, NULL will be
    // returned instead. That just means that no information has been loaded yet.
    public Account getAccountForPlayer(Player player) {
        return mPlayerAccountMap.get(player);
    }
}
