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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
    private ChatManager mChatManager;
    
    public EventListener(EventDispatcher eventDispatcher, AccountManager accountManager, ChatManager chatManager) {
        mEventDispatcher = eventDispatcher;
        mAccountManager = accountManager;
        mChatManager = chatManager;
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
     * Invoked when a player chats with other players. We route all incoming chat messages through
     * the ChatManager to make sure we can filter it, before dispatching it to features.
     * 
     * This event is being called "asynchronous" by Bukkit. By this they mean that it gets executed
     * on another thread, but it still gives us the ability to cancel the event if we want. Features
     * which listen to this event should not call the Bukkit API in their handlers.
     * 
     * @param event The Bukkit AsyncPlayerChatEvent object.
     */
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        mChatManager.onIncomingMessage(event,
                                       mAccountManager.getAccountForPlayer(event.getPlayer()),
                                       mEventDispatcher);
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
