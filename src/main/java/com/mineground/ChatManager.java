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

import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.mineground.account.Account;

// The Chat Manager is the entry point for all communication in Mineground. It receives the incoming
// events from the EventListener, which also shares the account of the player who sent the message.
// Feature can set filters on the chat manager to only be informed about a subset of communication,
// or they can listen to the onPlayerChat event which will receive the remainder.
public class ChatManager {
    public ChatManager() {
    }
    
    // Called when any player has sent a chat message to the server. Apply the filters and determine
    // whether the Bukkit event should be blocked, and the chat shouldn't be forwarded to others.
    public void onIncomingMessage(AsyncPlayerChatEvent event, Account account, EventDispatcher eventDispatcher) {
        // TODO: Do magic here.
        
        eventDispatcher.onPlayerChat(event.getPlayer(), event.getMessage());
    }
}
