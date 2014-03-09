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
import com.mineground.base.Color;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Message;

// When a player joins or leaves Mineground, we'd like to welcome them and inform the other players
// about their presence. Furthermore, when a player joins Mineground for the first time, we may want
// to be a little bit nicer and give them some money and inventory to start with.
public class PlayerSessionMessages extends FeatureBase {
    // The amount of money a player will receive when they join Mineground for the first time.
    private static final int MONEY_GIFT_ON_FIRST_JOIN = 100;

    // Message used for welcoming players to Mineground when they join for the first time.
    private final Message mWelcomeFirstTimeMessage;
    
    public PlayerSessionMessages(FeatureInitParams params) {
        super(params);
        
        mWelcomeFirstTimeMessage = Message.Load("first_join");
        mWelcomeFirstTimeMessage.setInteger("money_gift", MONEY_GIFT_ON_FIRST_JOIN);
    }
    
    public void onPlayerJoined(Player player) {
        // TODO: Distribute a message about this player's join to all players.
        // TODO: Don't send a message if the Mineground plugin is being loaded.
        
        final Account account = getAccountManager().getAccountForPlayer(player);
        if (account.isFirstJoin()) {
            // TODO: We need to differentiate between a player's first join ever, and their first
            //       join as a builder. We need to hook in to the registration system for this..

            mWelcomeFirstTimeMessage.setString("nickname", player.getName());
            mWelcomeFirstTimeMessage.send(player, Color.YELLOW);
        }
    }
    
    public void onPlayerQuit(Player player) {
        // TODO: Distribute a message about the player's quit to all players.
        // TODO: Don't send a message if the Mineground plugin is being unloaded.
    }
}
