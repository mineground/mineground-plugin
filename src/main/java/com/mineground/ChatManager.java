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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.mineground.account.Account;
import com.mineground.account.AccountManager;
import com.mineground.base.Color;
import com.mineground.base.Message;

/**
 * The Chat Manager is the entry point for all communication in Mineground. It receives the incoming
 * events from the EventListener, which also shares the account of the player who sent the message.
 * Feature can set filters on the chat manager to only be informed about a subset of communication,
 * or they can listen to the onPlayerChat event which will receive the remainder.
 */
public class ChatManager {
    /**
     * The account manager provides us with information about which players are part of certain
     * groups, which is being used to distribute messages to only a sub-set of players.
     */
    private final AccountManager mAccountManager;
    
    /**
     * Message format used to announce a message to members of Mineground's staff.
     */
    private final Message mStaffAnnouncementMessage;
    
    public ChatManager(AccountManager accountManager) {
        mAccountManager = accountManager;
        
        mStaffAnnouncementMessage = Message.Load("staff_announcement");
    }
    
    /**
     * Distributes |message| as an announcement intended for staff members. The |permission|
     * parameter may be used to only send this to staff members with a certain permission.
     * 
     * @param message       The message which should be distributed to the staff.
     * @param permission    The permission a staff member must have in order to receive this.
     */
    public void distributeStaffAnnouncement(Message message, String permission) {
        List<Player> targets = new ArrayList<Player>();
        for (Player player : mAccountManager.getOnlineStaff()) {
            if (permission != null && !player.hasPermission(permission))
                continue;
            
            targets.add(player);
        }
        
        message.send(targets, Color.PLAYER_EVENT);
    }
    
    /**
     * Distributes |message| as an announcement intended for staff members.
     * 
     * @param message   The message which should be distributed to the staff.
     */
    public void distributeStaffAnnouncement(Message message) {
        distributeStaffAnnouncement(message, null);
    }
    
    /**
     * Distributes |message| as an announcement intended for staff members. The |permission|
     * parameter may be used to only send this to staff members with a certain permission.
     * 
     * @param message       The message which should be distributed to the staff.
     * @param permission    The permission a staff member must have in order to receive this.
     */
    public void distributeStaffAnnouncement(String message, String permission) {
        mStaffAnnouncementMessage.setString("message", message);
        distributeStaffAnnouncement(mStaffAnnouncementMessage, permission);
    }
    
    /**
     * Distributes |message| as an announcement intended for staff members.
     * 
     * @param message   The message which should be distributed to the staff.
     */
    public void distributeStaffAnnouncement(String message) {
        mStaffAnnouncementMessage.setString("message", message);
        distributeStaffAnnouncement(mStaffAnnouncementMessage, null);
    }
    
    /**
     * Called when any player has sent a chat message to the server. Apply the filters and determine
     * whether the Bukkit event should be blocked, and the chat shouldn't be forwarded to others.
     * 
     * @param event             The Bukkit event which was fired
     * @param account           Account of the player who sent the message.
     * @param eventDispatcher   Event dispatcher which will be used to inform the plugin.
     */
    // 
    public void onIncomingMessage(AsyncPlayerChatEvent event, Account account, EventDispatcher eventDispatcher) {
        // TODO: Do magic here.
        
        eventDispatcher.onPlayerChat(event.getPlayer(), event.getMessage());
    }
}
