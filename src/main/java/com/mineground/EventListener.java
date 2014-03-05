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

package com.mineground;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mineground.account.AccountManager;

// The Event Listener class is responsible for listening to incoming Bukkit plugins which we'd like
// to have handled within Mineground. It will change some of the semantics of the events, for
// example changing some of Bukkit's types to our own wrapper types.
public class EventListener implements Listener {
    private EventDispatcher mEventDispatcher;
    private AccountManager mAccountManager;
    
    public EventListener(EventDispatcher eventDispatcher, AccountManager accountManager) {
        mEventDispatcher = eventDispatcher;
        mAccountManager = accountManager;
    }
    
    // Invoked when a player joins the server, and the PlayerLoginEvent has succeeded. Mineground
    // considers this the time at which a player's connection can be considered successful. However,
    // since they haven't logged in to their account yet, they will be considered a a guest.
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerJoined(PlayerJoinEvent event) {
        mAccountManager.loadAccount(event.getPlayer(), mEventDispatcher);
    }
    
    // Invoked when a player leaves the server. Features may want to finalize their information, and
    // the account manager should be given the opportunity to store their account to the database.
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        mEventDispatcher.onPlayerQuit(event.getPlayer());
        mAccountManager.unloadAccount(event.getPlayer());
    }
}
