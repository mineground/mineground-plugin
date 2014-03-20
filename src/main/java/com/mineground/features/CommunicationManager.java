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

package com.mineground.features;

import java.util.List;

import org.bukkit.entity.Player;

import com.mineground.account.Account;
import com.mineground.base.Color;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Message;

/**
 * The communication manager is responsible for the different communication channels, e.g. normal,
 * gang and staff chat, as well as various commands related to private messaging.
 */
public class CommunicationManager extends FeatureBase {
    /**
     * Prefix used for only distributing the following message to administrators. All players can
     * send messages to this channel, but only staff members can read them.
     */
    private static final String StaffCommunicationPrefix = "@";
    
    /**
     * Instance of the CommunicationCommandsCommands class, which implements all the commands
     * related to communication on Mineground.
     */
    @SuppressWarnings("unused")
    private final CommunicationCommands mCommands;
    
    /**
     * Message containing the format in which staff chat will be displayed.
     */
    private final Message mStaffChatMessage;
    
    /**
     * Message displayed to a non-staff user when they contacted the online staff.
     */
    private final Message mStaffNotifiedMessage;
    
    /**
     * Message displayed to a non-staff user when sending a message to staff members, while no
     * staff members are currently online on Mineground.
     */
    private final Message mStaffOfflineMessage;
    
    public CommunicationManager(FeatureInitParams params) {
        super(params);
        
        // Initialize the commands component of the Communication Manager.
        mCommands = new CommunicationCommands(this, params);
        
        mStaffChatMessage = Message.Load("staff_chat_format");
        mStaffNotifiedMessage = Message.Load("staff_notified");
        mStaffOfflineMessage = Message.Load("no_staff_online");
    }
    
    /**
     * Receives and outputs chat messages sent to the server by players. If the message was sent to
     * a specific channel (and thus prefixed with the channel's identifier), only a subset of the
     * players will receive it. Otherwise everyone will.
     * 
     * @param player    The player who sent a chat message.
     * @param message   The message which they wrote.
     */
    public void onPlayerChat(Player player, String message) {
        if (message.startsWith(StaffCommunicationPrefix)) {
            List<Player> staff = getAccountManager().getOnlineStaff();
            if (staff.size() > 0) {
                mStaffChatMessage.setString("nickname", player.getName());
                mStaffChatMessage.setString("message", message.substring(1));
                mStaffChatMessage.send(staff, Color.PLAYER_EVENT);
                
                if (!staff.contains(player)) {
                    mStaffNotifiedMessage.send(player, Color.GREEN);
                }
            } else {
                mStaffOfflineMessage.send(player, Color.ACTION_REQUIRED);
            }
            
            // TODO: Send this message to IRC so that folks there can read.
            return;
        }

        final Account account = getAccountManager().ensureAuthenticatedAccount(player);
        if (account == null)
            return;
        
        // TODO: Implement support for group chat.
        // TODO: Implement support for filters?
        
        StringBuilder messageBuilder = new StringBuilder();
        // TODO: Append the player's chat prefix (group or level).
        messageBuilder.append("<");
        messageBuilder.append(player.getName());
        messageBuilder.append("> ");
        messageBuilder.append(message);
        
        getServer().broadcastMessage(messageBuilder.toString());
    }

}
