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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.EventDispatcher;
import com.mineground.base.Color;
import com.mineground.base.CommandHandler;
import com.mineground.base.Message;
import com.mineground.base.PasswordHash;
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
import com.mineground.database.Database;

// The account manager curates the player accounts on Mineground, their statistics, information,
// economics and everything else related to it. Each player implicitly gets an account, however,
// without having registered their account on the website it will be considered as a guest account.
public class AccountManager {
    // The maximum number of times a player can try to identify to an account using /login.
    private static final int MAXIMUM_AUTHENTICATION_ATTEMPTS = 3;
    
    // The maximum number of hours the player's last session must be in the past in order for IP-
    // based automatic login to be available for their account.
    private static final int MAXIMUM_AUTOMATIC_LOGIN_HOURS = 24;
    
    // Interface between the account manager and the database.
    private final AccountDatabase mAccountDatabase;
    
    // Map between Bukkit players and their Mineground accounts.
    private final Map<Player, Account> mPlayerAccountMap;
    
    // The message which will be send to every connecting player, regardless of their account.
    private final Message mConnectionMessage;
    
    // The message which will be send to the player when authentication requires their password.
    private final Message mRequirePasswordMessage;
    
    // The message which will be send to the player when they entered an invalid password.
    private final Message mInvalidPasswordMessage;
    
    // Class containing information about a player who has connected to Mineground, but has not yet
    // authenticated themselves with their account.
    class PendingAuthentication {
        public AccountData accountData;
        public EventDispatcher dispatcher;
        public int attempts;
        
        PendingAuthentication(AccountData accountData_, EventDispatcher dispatcher_) {
            accountData = accountData_;
            dispatcher = dispatcher_;
            attempts = 0;
        }
    }
    
    // Map between Bukkit players and their account data, for players whose authentication is still
    // pending on them entering their password using the /login command.
    private final Map<Player, PendingAuthentication> mAuthenticationRequestMap;
    
    public AccountManager(Database database) {
        mAccountDatabase = new AccountDatabase(database);
        mPlayerAccountMap = new HashMap<Player, Account>();
        mAuthenticationRequestMap = new HashMap<Player, PendingAuthentication>();
        
        mConnectionMessage = Message.Load("welcome");
        mRequirePasswordMessage = Message.Load("login_password");
        mInvalidPasswordMessage = Message.Load("login_invalid");
    }
    
    // Loads the account, and don't fire the onPlayerJoined event on the dispatcher until their
    // information has been loaded and verified. This will be called for all online players when the
    // plugin is being loaded while there already are players in-game.
    public void loadAccount(final Player player, final EventDispatcher dispatcher) {
        // TODO: Don't send a message to the player when we're reloading the plugin.
        mConnectionMessage.send(player, Color.GOLD);
        
        mPlayerAccountMap.put(player,  new Account());
        mAccountDatabase.loadOrCreateAccount(player).then(new PromiseResultHandler<AccountData>() {
            public void onFulfilled(AccountData accountData) {
                if (!player.isOnline())
                    return;

                // If the player's account does not have a password set, they're a guest and we
                // won't be able to log them in to any account. Bail out.
                if (accountData.password.length() == 0) {
                    didAuthenticatePlayer(player, accountData, dispatcher);
                    return;
                }
                
                // Try to authenticate the player with their account information. We will not
                // authenticate the player with their account until we know it's really them.
                authenticatePlayerAccount(player, accountData, dispatcher);
            }

            // Something went wrong when trying to create an account for this player.
            public void onRejected(PromiseError error) {
                if (!player.isOnline())
                    return;
                
                player.sendMessage(Color.SCRIPT_ERROR + "Your account could not be loaded from the database.");
                player.sendMessage(Color.SCRIPT_ERROR + "Please contact an administrator to get this resolved!");
                dispatcher.onPlayerJoined(player);
            }
        });
    }
    
    // Authenticates |player| based on their |accountData|. The player will need to enter their
    // password in order to identify their identity, unless their IP address matches their last one,
    // and their previous session was in the near past.
    //
    // If Mineground were to switch to be an online server, we could do silent authentication here
    // by comparing their unique Id (the minecraft.net Id) against the one in the database.
    private void authenticatePlayerAccount(final Player player, final AccountData accountData, final EventDispatcher dispatcher) {
        final Account account = mPlayerAccountMap.get(player);
        if (account == null)
            return;
        
        long previousSessionTimeAgo = ((new Date().getTime()) - accountData.last_seen.getTime());
        if (previousSessionTimeAgo < (MAXIMUM_AUTOMATIC_LOGIN_HOURS * 60 * 60 * 1000)) {
            // The player's last session was less than |MAXIMUM_AUTOMATIC_LOGIN_HOURS| hours ago, so
            // if their IP address matches we will automatically log them in again.
            final String ipAddress = player.getAddress().getAddress().toString();
            if (accountData.last_ip != null && accountData.last_ip == ipAddress) {
                didAuthenticatePlayer(player, accountData, dispatcher);
                return;
            }
        }
        
        // We cannot log them in automatically. They now have to use the /login command with their
        // password before they will be allowed to participate in playing on Mineground.
        mAuthenticationRequestMap.put(player, new PendingAuthentication(accountData, dispatcher));
        mRequirePasswordMessage.send(player, Color.ACTION_REQUIRED);
    }
    
    // Users have to identify with their account using the /login command, using which they specify
    // their password. This method handles input for that command. If we can verify the player's
    // password, we'll continue to authenticate the player with their account.
    @CommandHandler("login")
    public boolean onLoginCommand(CommandSender sender, Command command, String[] arguments) {
        if (!(sender instanceof Player) || arguments.length == 0)
            return false;
        
        final Player player = (Player) sender;

        final PendingAuthentication authenticationRequest = mAuthenticationRequestMap.get(player);
        if (authenticationRequest == null) {
            player.sendMessage("Either you are already logged in, or your account is not yet available!");
            return true;
        }
        
        if (++authenticationRequest.attempts >= MAXIMUM_AUTHENTICATION_ATTEMPTS) {
            // TODO: Log this with the PlayerLog class.
            // TODO: Show a message to the user about what to do when they forgot their password.
            player.kickPlayer("Too many invalid passwords.");
            return true;
        }
        
        final Account account = mPlayerAccountMap.get(player);
        if (account == null)
            throw new RuntimeException("|account| must not be NULL here.");
        
        String password = arguments[0];
        try {
            if (!PasswordHash.validatePassword(password, authenticationRequest.accountData.password)) {
                mInvalidPasswordMessage.send(player, Color.ACTION_REQUIRED);
            } else {
                didAuthenticatePlayer(player, authenticationRequest.accountData, authenticationRequest.dispatcher);
                mAuthenticationRequestMap.remove(player);
            }   
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // This is very bad -- it means the server does not support the PBKDF2 password
            // algorithm. We can't recover from this, given that's how we hash all the passwords..
            player.sendMessage(Color.SCRIPT_ERROR + "PBKDF2 is not available on the server, please notify an admin!");
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // This is very bad -- it means the PasswordHash implementation threw an exception for
            // other reasons, e.g. because the stored hash is invalid.
            player.sendMessage(Color.SCRIPT_ERROR + "The password algorithm crashed, please notify an admin!");
            e.printStackTrace();
        }
        
        return true;
    }
    
    // Will set up the player's state properly when they've been recognized as the rightful owner
    // of their account. Called both for automatic identification, and for the /login command.
    private void didAuthenticatePlayer(Player player, AccountData accountData, EventDispatcher dispatcher) {
        final Account account = mPlayerAccountMap.get(player);
        if (account == null)
            throw new RuntimeException("|account| must not be NULL here.");

        // TODO: Log this with the PlayerLog class.
        account.load(accountData);
        
        dispatcher.onPlayerJoined(player);
    }

    // Called when the player is leaving the server, meaning we should store the latest updates to
    // their account in the database. When the Mineground plugin is disabled, this method will be
    // called for all players to ensure that we properly store all information.
    public void unloadAccount(Player player) {
        mAuthenticationRequestMap.remove(player);

        final Account account = mPlayerAccountMap.get(player);
        if (account == null)
            return;
        
        mPlayerAccountMap.remove(player);
        
        final AccountData accountData = account.getAccountData();
        if (accountData == null)
            return;
        
        mAccountDatabase.updateAccount(accountData, player);
    }
    
    // Retrieves the account for |player|. If no account is available for them, NULL will be
    // returned instead. That just means that no information has been loaded yet.
    public Account getAccountForPlayer(Player player) {
        return mPlayerAccountMap.get(player);
    }
}
