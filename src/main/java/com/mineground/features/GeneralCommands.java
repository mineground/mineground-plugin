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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.account.AccountLevel;
import com.mineground.account.PlayerLog;
import com.mineground.account.PlayerLog.RecordType;
import com.mineground.base.Color;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Message;

/**
 * Various generic commands may be implemented in this class.
 */
public class GeneralCommands extends FeatureBase {
    /**
     * The number of server ticks forced weather using the /weather command should last for. One
     * second will equal roughly 20 server ticks.
     */
    private final static int FORCED_WEATHER_DURATION_TICKS = 12000; // 10 minutes (600 seconds).
    
    /**
     * The number of server ticks forced thunder should last for.
     */
    private final static int FORCED_THUNDER_DURATION_TICKS = 3600; // 3 minutes (180 seconds).
    
    /**
     * Message containing the server rules, which players can view using the /rules command.
     */
    private final Message mRulesMessage;
    
    /**
     * Message which will be send to users when somebody changes the weather in the world they're
     * currently playing in.
     */
    private final Message mWeatherChangeMessage;
    
    /**
     * Message which will be send to users when somebody changes the time in the world they're
     * currently playing in.
     */
    private final Message mTimeChangeMessage;
    
    public GeneralCommands(FeatureInitParams params) {
        super(params);
        
        mRulesMessage = Message.Load("server_rules");
        mWeatherChangeMessage = Message.Load("weather_change");
        mTimeChangeMessage = Message.Load("time_change");
    }

    /**
     * Displays a list of in-game staff members to the player. The staff will be grouped together
     * based on their level, meaning that Management will be listed first, moderators last.
     * 
     * @param sender    The player who would like to know about online staff.
     * @param arguments Additional arguments passed on to this method. Ignored.
     */
    @CommandHandler(value = "staff", aliases = { "admins" }, console = true, remote = true)
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
    
    /**
     * Displays Mineground's server rules to <code>sender</code>.
     * 
     * @param sender    The player who likes to view the server rules.
     * @param arguments Additional arguments given to the command. Ignored.
     */
    @CommandHandler("rules")
    public void onRulesCommand(CommandSender sender, String[] arguments) {
        sender.sendMessage(Color.GOLD + "----------------------- Rules ------------------------");
        mRulesMessage.send(sender, Color.WHITE);
        sender.sendMessage(Color.GOLD + "-----------------------------------------------------");
    }
    
    /**
     * Changes the weather in the sender's current world. When the sender has no world, weather in
     * the main and creative worlds will be changed instead.
     * 
     * @param sender    The player, console or user wanting to change the weather.
     * @param arguments Arguments passed. One is expected, the weather type.
     */
    @CommandHandler(value = "weather", console = true, remote = true)
    public void onWeatherCommand(CommandSender sender, String[] arguments) {
        if (!sender.hasPermission("command.weather")) {
            displayCommandError(sender, "You don't have permission to use the /weather command yet.");
            return;
        }
        
        if (arguments.length == 0) {
            displayCommandUsage(sender, "/weather [sun/rain/storm]");
            return;
        }
        
        // Find a list of worlds which the weather change should apply to, and a set of players who
        // are currently residing in those worlds. Only these players will receive a message.
        final List<World> worlds = new ArrayList<World>();
        final Set<Player> players = new HashSet<Player>();

        if (sender instanceof Player) {
            worlds.add(((Player) sender).getWorld());
        } else {
            final WorldManager worldManager = (WorldManager) getFeatureManager().getFeature("WorldManager");
            if (worldManager != null) {
                worlds.add(worldManager.getDefaultWorld());
                worlds.add(worldManager.getCreativeWorld());
            }
        }
        
        for (World world : worlds)
            players.addAll(world.getPlayers());

        // Updates the weather to be sunny. This is the type of weather most players prefer.
        if (arguments[0].equals("sun")) {
            for (World world : worlds) {
                world.setStorm(false);
                world.setWeatherDuration(FORCED_WEATHER_DURATION_TICKS);
            }
        
        // Rain speaks for itself. When the temperature in a given region is below a threshold, or
        // the region is far up in the sky, it will start snowing instead.
        } else if (arguments[0].equals("rain")) {
            for (World world : worlds) {
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(FORCED_WEATHER_DURATION_TICKS);
            }
        
        // Storm is the most exciting weather as it includes thunder, but this could potentially
        // damage property and cause fires. The weather type therefore requires another permission.
        } else if (arguments[0].equals("storm")) {
            if (!sender.hasPermission("command.weather.storm")) {
                displayCommandError(sender, "You don't have permission to change the weather to a storm.");
                return;
            }
            
            for (World world : worlds) {
                world.setStorm(true);
                world.setThundering(true);
                world.setThunderDuration(FORCED_THUNDER_DURATION_TICKS);
                world.setWeatherDuration(FORCED_WEATHER_DURATION_TICKS);
            }
        } else {
            displayCommandUsage(sender, "/weather [sun/rain/storm]");
            return;
        }
        
        // Send a message to all players in those worlds about the change having happened. This will
        // avoid them being all confused about the sudden influx of thunder.
        mWeatherChangeMessage.setString("nickname", sender.getName());
        mWeatherChangeMessage.setString("weather", arguments[0]);
        mWeatherChangeMessage.send(players, Color.PLAYER_EVENT);
    }
    
    /**
     * Command which allows certain players to change the time in the world they're currently in.
     * When this command gets invoked from the console or from IRC, the time will be changed in the
     * default and creative worlds instead.
     * 
     * @param sender    The player, console or user wanting to change the weather.
     * @param arguments Arguments passed. One is expected, the new time.
     */
    @CommandHandler(value = "time", console = true, remote = true)
    public void onTimeCommand(CommandSender sender, String[] arguments) {
        if (!sender.hasPermission("command.time")) {
            displayCommandError(sender, "You don't have permission to use the /time command yet.");
            return;
        }
        
        if (arguments.length == 0) {
            displayCommandUsage(sender, "/time [morning/day/evening/night]");
            return;
        }
        
        // Find a list of worlds which the time change should apply to, and a set of players who are
        // currently residing in those worlds. Only these players will receive a message.
        final List<World> worlds = new ArrayList<World>();
        final Set<Player> players = new HashSet<Player>();

        if (sender instanceof Player) {
            worlds.add(((Player) sender).getWorld());
        } else {
            final WorldManager worldManager = (WorldManager) getFeatureManager().getFeature("WorldManager");
            if (worldManager != null) {
                worlds.add(worldManager.getDefaultWorld());
                worlds.add(worldManager.getCreativeWorld());
            }
        }
        
        for (World world : worlds)
            players.addAll(world.getPlayers());
        
        // Find the time which it should be in each of the worlds. These numbers feel awkwardly
        // arbitrary to me, but this is how Minecraft's day-and-night cycles work..
        Integer time = null;
        if (arguments[0].equals("morning"))
            time = 22500;
        else if (arguments[0].equals("day"))
            time = 1000;
        else if (arguments[0].equals("evening"))
            time = 12000;
        else if (arguments[0].equals("night"))
            time = 18000;
        else {
            displayCommandUsage(sender, "/time [morning/day/evening/night]");
            return;
        }

        // Update the time in each of the worlds we're updating.
        for (World world : worlds)
            world.setTime(time);
        
        // Distribute the time changed message to all players in those worlds. This will inform them
        // of the change, who made it, and allow them to object to those players who insist on it
        // being day throughout their playing sessions (while night is cool!).
        mTimeChangeMessage.setString("nickname", sender.getName());
        mTimeChangeMessage.setString("time", arguments[0]);
        mTimeChangeMessage.send(players, Color.PLAYER_EVENT);
    }
}
