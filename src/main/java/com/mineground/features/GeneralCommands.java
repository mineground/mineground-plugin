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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.account.AccountLevel;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;

/**
 * Various generic commands may be implemented in this class. In order to keep this class reasonably
 * workable, commands implemented in here must not need any members or helper functions.
 */
public class GeneralCommands extends FeatureBase {
    public GeneralCommands(FeatureInitParams params) { super(params); }

    /**
     * Displays a list of in-game staff members to the player. The staff will be grouped together
     * based on their level, meaning that Management will be listed first, moderators last.
     * 
     * @param sender    The player who would like to know about online staff.
     * @param arguments Additional arguments passed on to this method. Ignored.
     */
    @CommandHandler(value = "staff", aliases = { "admins" }, console = true)
    public void onStaffCommand(CommandSender sender, String[] arguments) {
        Map<AccountLevel, List<String>> groups = new EnumMap<AccountLevel, List<String>>(AccountLevel.class);
        for (Player player : getAccountManager().getOnlineStaff()) {
            AccountLevel level = getAccountForPlayer(player).getLevel();
            if (!groups.containsKey(level))
                groups.put(level, new ArrayList<String>());
            
            groups.get(level).add(player.getName());
        }
        
        // Bail out now if there are no staff members in-game.
        if (groups.size() == 0) {
            displayCommandError(sender, "There are no staff members online right now.");
            return;
        }
        
        displayCommandSuccess(sender, "The following staff members are online:");
        
        StringBuilder message = new StringBuilder();
        for (AccountLevel level : Arrays.asList(AccountLevel.Management, AccountLevel.Administrator, AccountLevel.Moderator)) {
            List<String> players = groups.get(level);
            if (players == null)
                return;
            
            message.append(AccountLevel.colorFor(level));
            message.append(AccountLevel.toString(level));
            message.append("§f: ");
            
            Collections.sort(players);
            for (String playerName : players)
                message.append(playerName).append(", ");
            
            // Remove the final ", ", which shouldn't be part of the message.
            message.setLength(message.length() - 2);
            
            // Share the message with the party (player or console) who requested this.
            sender.sendMessage(message.toString());
            
            // Clear the message so it can be used for the next level, without reallocating.
            message.setLength(0);
        }
    }
}
