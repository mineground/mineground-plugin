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

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mineground.account.AccountManager;

/**
 * The Event Listener class is responsible for listening to incoming Bukkit plugins which we'd like
 * to have handled within Mineground. It will change some of the semantics of the events, for
 * example changing some of Bukkit's types to our own wrapper types.
 */
public class EventListener implements Listener {
    private EventDispatcher mEventDispatcher;
    private AccountManager mAccountManager;
    
    public EventListener(EventDispatcher eventDispatcher, AccountManager accountManager) {
        mEventDispatcher = eventDispatcher;
        mAccountManager = accountManager;
    }
    
    /**
     * Invoked when a player joins the server, and the PlayerLoginEvent has succeeded. Mineground
     * considers this the time at which a player's connection can be considered successful. However,
     * since they haven't logged in to their account yet, they will be considered a a guest.
     * 
     * @param event The Bukkit PlayerJoinEvent object.
     */
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerJoined(PlayerJoinEvent event) {
        mAccountManager.loadAccount(event.getPlayer(), mEventDispatcher);
        event.setJoinMessage(null);
    }
    
    /**
     * Invoked when a player on the server moves, which can be once per player per tick. The account
     * manager will be consulted to see if the player is allowed to move yet. They may look around.
     * 
     * @param event The Bukkit PlayerMoveEvent object.
     */
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!mAccountManager.ensureAuthenticated(event.getPlayer())) {
            final Location from = event.getFrom();
            final Location to = event.getTo();
            
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                from.setPitch(to.getPitch());
                from.setYaw(to.getYaw());
                
                event.setTo(from);
                return;
            }
        }
        
        // TODO: Distribute this event within Mineground if we need to.
    }
    
    /**
     * Invoked when a player chats with other players. All features will be able to receive all
     * incoming chat messages, but should check whether the player is logged in manually.
     * 
     * This event is being called "asynchronous" by Bukkit. By this they mean that it gets executed
     * on another thread, but it still gives us the ability to cancel the event if we want. Features
     * which listen to this event should not call the Bukkit API in their handlers.
     * 
     * @param event The Bukkit AsyncPlayerChatEvent object.
     */
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled())
            return;

        event.setCancelled(true);
        mEventDispatcher.onPlayerChat(event.getPlayer(), event.getMessage());
    }
    
    /**
     * Invoked when a player dies. The reason is not directly included in the event, but it should
     * be available when reading the cause of the last damage occurred to the player. 
     * 
     * @param event The Bukkit PlayerDeathEvent object.
     */
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        
        // TODO: There would be too many parameters (4 or 5) to pass along if we weren't passing the
        //       Bukkit event directly. Should we just do this everywhere? Only if it makes sense?
        mEventDispatcher.onPlayerDeath(event);
    }
    
    /**
     * Invoked when a player leaves the server. Features may want to finalize their information, and
     * the account manager should be given the opportunity to store their account to the database.
     * 
     * @param event The Bukkit PlayerQuitEvent object.
     */
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        mEventDispatcher.onPlayerQuit(event.getPlayer());
        mAccountManager.unloadAccount(event.getPlayer());
        event.setQuitMessage(null);
    }
}
