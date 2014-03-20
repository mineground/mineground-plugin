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

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.account.AccountLevel;
import com.mineground.account.PlayerLog;
import com.mineground.account.PlayerLog.RecordType;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;

/**
 * Various generic commands may be implemented in this class.
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
    
    /**
     * SBuilders and higher have the ability to toggle whether to fly around in the world. This can
     * be done through the /fly command. It's automatically enabled for all players in creative.
     * 
     * @param sender    The player who is planning on flying about.
     * @param arguments Additional arguments (booleansy arguments are accepted).
     */
    @CommandHandler("fly")
    public void onFlyCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        if (player.getGameMode() == GameMode.CREATIVE) {
            displayCommandError(player, "You're currently playing in creative mode, so flying is already possible!");
            return;
        }
        
        if (!player.hasPermission("command.fly")) {
            displayCommandError(player, "Sorry, you don't have permission yet to use the /fly command.");
            return;
        }
        
        boolean enableFlying = !player.getAllowFlight();
        if (arguments.length > 0)
            enableFlying = argumentAsBoolean(arguments[0]);
        
        PlayerLog.record(RecordType.COMMAND_FLY, getUserId(player), enableFlying ? 1 : 0);
        player.setAllowFlight(enableFlying);
        
        displayCommandSuccess(player, "You have " + (enableFlying ? "enabled" : "disabled") + " the ability to fly!");
    }
    
    /**
     * Tries to interpret |argument| as a GameMode type. If no exact type could be found, then NULL
     * will be returned instead.
     * 
     * @param argument  The argument to interpret as a GameMode type.
     * @return          The GameMode it could map to, or NULL.
     */
    private GameMode argumentAsGameMode(String argument) {
        if (argument.equalsIgnoreCase("adventure"))
            return GameMode.ADVENTURE;
        
        if (argument.equalsIgnoreCase("creative"))
            return GameMode.CREATIVE;
        
        if (argument.equalsIgnoreCase("survival"))
            return GameMode.SURVIVAL;
        
        return null;
    }
    
    /**
     * Administrators and Management members have the ability to enable creative mode for both
     * themselves and for other players, in any world, regardless of the gamemode of that world.
     * 
     * By default, /mode will toggle between survival and creative mode. However, it's also possible
     * to move to adventure mode by passing that as an argument.
     * 
     * /mode [survival, creative, adventure]
     * /mode [player] [survival, creative, adventure]
     * 
     * @param sender    The player who intends to change their mode.
     * @param arguments Additional arguments passed on.
     */
    @CommandHandler("mode")
    public void onModeCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        if (!player.hasPermission("command.mode")) {
            displayCommandError(player, "Sorry, you don't have permission to use the /mode command.");
            return;
        }
        
        GameMode targetMode = null;
        Player targetPlayer = null;
        
        if (arguments.length >= 1) {
            targetPlayer = getServer().getPlayer(arguments[0]);
            if (targetPlayer != null && arguments.length >= 2)
                targetMode = argumentAsGameMode(arguments[1]);
            else if (targetPlayer == null)
                targetMode = argumentAsGameMode(arguments[0]);
        }
        
        if (targetPlayer == null)
            targetPlayer = player;
        
        if (targetMode == null) {
            targetMode = targetPlayer.getGameMode() == GameMode.CREATIVE ?
                    GameMode.SURVIVAL : GameMode.CREATIVE;
        }
        
        targetPlayer.setGameMode(targetMode);
        
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("§2The gamemode of §a");
        messageBuilder.append(targetPlayer.getName());
        messageBuilder.append(" §2has been updated to §a");
        
        if (targetMode == GameMode.ADVENTURE)
            messageBuilder.append("adventure");
        else if (targetMode == GameMode.CREATIVE)
            messageBuilder.append("creative");
        else if (targetMode == GameMode.SURVIVAL)
            messageBuilder.append("survival");
        
        messageBuilder.append("§2.");
        
        // TODO: Inform other administrators about this.
        // TODO: Record usage of the /mode command.
        
        player.sendMessage(messageBuilder.toString());
    }
}
