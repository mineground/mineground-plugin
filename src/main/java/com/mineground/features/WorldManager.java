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

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.account.Account;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;

/**
 * Minecraft supports an arbitrary amount of worlds existing simultaneously, each with their own
 * settings and rules. Management members have the ability to create, remove and change various
 * critical settings of these rules, while each player will have the ability to teleport back and
 * forth between certain ones of them.
 */
public class WorldManager extends FeatureBase {
    public WorldManager(FeatureInitParams params) { super(params); }
    
    /**
     * The /world command is the primary entry point for Management members to manipulate the worlds
     * available on Mineground. New worlds can be created, current worlds can be renamed and removed
     * and settings of rules can be adjusted at their discretion.
     * 
     * /world                   Displays usage information for the /world command.
     * /world list              Lists the existing worlds on Mineground.
     * /world set               Lists options which can be set for the current world.
     * /world set spawn         Displays the spawn position. A value is necessary to change it.
     * /world set spawn here    Changes the spawn position to the current location of the player.
     * 
     * @param sender    The player who executed this command.
     * @param arguments The arguments which they passed on whilst executing.
     */
    @CommandHandler("world")
    public void onWorldCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        final Account account = getAccountForPlayer(player);
        final World world = player.getWorld();

        if (account == null || !player.hasPermission("world.list")) {
            displayCommandError(player, "You don't have permission to execute this command.");
            return;
        }
        
        // TODO: Implement /world list.
        
        // The /world set command exposes a wide variety of functions available to change settings
        // of the world the player is currently in. While more basic features such as the time and
        // weather can be control by more players, these are the more powerful settings.
        if (arguments.length >= 1 && arguments[0].equals("set")) {
            // /world spawn displays the spawn coordinates of the world when called without passing
            // any further arguments. With "here" as the final argument, it will be updated. We do
            // this to be consistent with the other /world set commands.
            if (arguments.length >= 2 && arguments[1].equals("spawn")) {
                if (!player.hasPermission("world.set.spawn")) {
                    displayCommandError(player, "You don't have permission to change the world's spawn position.");
                    return;
                }
                
                if (arguments.length >= 3 && arguments[2].equals("here")) {
                    Location location = player.getLocation();
                    if (!world.setSpawnLocation((int) location.getX(), (int) location.getY(), (int) location.getZ())) {
                        displayCommandError(player, "The spawn position could not be updated due to a Bukkit issue.");
                        return;
                    }
                    
                    // TODO: Announce to in-game staff that the spawn position has been changed.
                    
                    displayCommandSuccess(player, "The spawn position of this world has been changed!");
                    return;
                }
                
                Location location = world.getSpawnLocation();
                displayCommandDescription(player, "The spawn position is located at " +
                        "x:(" + (int) location.getX() + "), " +
                        "y:(" + (int) location.getY() + "), " +
                        "z:(" + (int) location.getZ() + ").");

                displayCommandDescription(player, "Execute \"/world set spawn here\" to update the spawn position.");
                return;
            }
            
            displayCommandUsage(player, "/world set [spawn]");
            displayCommandDescription(player, "Changes various settings related to worlds on Mineground.");
            return;
        }
        
        displayCommandUsage(player, "/world [list/set]");
        displayCommandDescription(player, "Creates and manages the worlds available on Mineground.");
    }
}
