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

import org.bukkit.entity.Player;

import com.mineground.account.Account;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;

/**
 * The communication manager is responsible for the different communication channels, e.g. normal,
 * gang and staff chat, as well as various commands related to private messaging.
 */
public class CommunicationManager extends FeatureBase {
    public CommunicationManager(FeatureInitParams params) {
        super(params);
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
        // TODO: Implement support for staff chat. Even unauthenticated players should be able to
        //       use it, as they need to be able to contact staff when they're stuck.
        
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
