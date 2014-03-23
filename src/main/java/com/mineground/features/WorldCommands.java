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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.account.Account;
import com.mineground.account.PlayerLog;
import com.mineground.account.PlayerLog.RecordType;
import com.mineground.base.Color;
import com.mineground.base.CommandCompletionHandler;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureComponent;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Message;
import com.mineground.features.WorldSettings.PvpSetting;

/**
 * Implementations of the commands associated with the world manager. These grant certain players
 * and staff members access to various options in regards to Minecraft worlds.
 */
public class WorldCommands extends FeatureComponent<WorldManager> {
    /**
     * The maximum number of entities which may be spawned per chunk in a world.
     */
    private final static int ENTITY_SPAWN_LIMIT = 150;
    
    /**
     * Map containing the game rule mappings supported by Mineground.
     */
    private final Map<String, String> mGameRulesMap;
    
    /**
     * Message for informing players that a world is being created on Mineground. This will cause
     * up to 30 seconds of lag, blocking all other kinds of playing.
     */
    private final Message mCreatingWorldMessage;
    
    /**
     * Message for informing players that the world has been created, and that they can continue
     * building their stuff again.
     */
    private final Message mWorldCreatedMessage;
    
    /**
     * Message to players who are in a world while it's being removed, and therefore are being
     * teleported back to the main spawn position on Mineground.
     */
    private final Message mWorldRemovedTeleportMessage;
    
    public WorldCommands(WorldManager manager, FeatureInitParams params) {
        super(manager, params);
        
        // The following map contains the game rules which may be changed using the /world set rule
        // command on Mineground. Update this map if Bukkit introduces new ones.
        mGameRulesMap = new HashMap<String, String>();
        mGameRulesMap.put("block-drops", "doTileDrops"); // whether breaking a block drops something.
        mGameRulesMap.put("command-block-notify", "commandBlockOutput"); // command block operations to inform ops?
        mGameRulesMap.put("daylight-cycle", "doDaylightCycle"); // whether the day/night cycle should be on.
        mGameRulesMap.put("fire-spread", "doFireTick"); // whether fire should spread between tiles.
        mGameRulesMap.put("keep-inventory", "keepInventory"); // whether to keep one's inventory on death.
        mGameRulesMap.put("mobs-damage", "mobGriefing"); // whether mobs can do damage to blocks.
        mGameRulesMap.put("mobs-loot", "doMobLoot"); // whether mobs should drop items.
        mGameRulesMap.put("mobs-spawn", "doMobSpawning"); // whether mobs should be spawned at all.
        mGameRulesMap.put("health-regeneration", "naturalRegeneration"); // whether to regenerate health.
        
        // Verify that all existing game-rules have got a mapping in the |mGameRulesMap| map. If
        // a new entry was added, display a warning in the server's console and add the value as
        // a mapping to itself, making new Minecraft functionality available as soon as possible.
        for (String gameRule : getServer().getWorlds().get(0).getGameRules()) {
            if (!mGameRulesMap.containsValue(gameRule)) {
                getLogger().warning("WorldManager: the game rule \"" + gameRule + "\" has not been defined.");
                mGameRulesMap.put(gameRule, gameRule);
            }
        }
        
        mCreatingWorldMessage = Message.Load("world_creation_start");
        mWorldCreatedMessage = Message.Load("world_creation_end");
        mWorldRemovedTeleportMessage = Message.Load("world_destroyed_teleport");
    }
    
    /**
     * Command which allows player to teleport to the classic world. If the player already is in
     * the classic world, this command will teleport them to the spawn position.
     * 
     * @param sender    The player to teleport to the classic world.
     * @param arguments Additional arguments. Ignored.
     */
    @CommandHandler("classic")
    public void onClassicCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        if (!player.hasPermission("command.classic")) {
            displayCommandError(player, "You don't have permission to use the /classic command.");
            return;
        }
        
        final World world = getFeature().getClassicWorld();
        if (world == null) {
            displayCommandError(player, "The classic world is unavailable right now, sorry!");
            return;
        }
        
        player.teleport(world.getSpawnLocation());
        
        displayCommandSuccess(player, "You have been teleported to the classic world.");
        return;
    }
    
    /**
     * Command which allows player to teleport to the creative world. If the player already is in
     * the creative world, this command will teleport them to the spawn position.
     * 
     * @param sender    The player to teleport to the creative world.
     * @param arguments Additional arguments. Ignored.
     */
    @CommandHandler("creative")
    public void onCreativeCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        if (!player.hasPermission("command.creative")) {
            displayCommandError(player, "You don't have permission to use the /creative command.");
            return;
        }
        
        final World world = getFeature().getCreativeWorld();
        if (world == null) {
            displayCommandError(player, "The creative world is unavailable right now, sorry!");
            return;
        }
        
        player.teleport(world.getSpawnLocation());
        
        displayCommandSuccess(player, "You have been teleported to the creative world.");
        return;
    }
    
    /**
     * Command which allows player to teleport to the survival world. If the player already is in
     * the survival world, this command will teleport them to the spawn position.
     * 
     * @param sender    The player to teleport to the survival world.
     * @param arguments Additional arguments. Ignored.
     */
    @CommandHandler("survival")
    public void onSurvivalCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        if (!player.hasPermission("command.survival")) {
            displayCommandError(player, "You don't have permission to use the /survival command.");
            return;
        }
        
        final World world = getFeature().getDefaultWorld();
        if (world == null) {
            displayCommandError(player, "The survival world is unavailable right now, sorry!");
            return;
        }
        
        player.teleport(world.getSpawnLocation());
        
        displayCommandSuccess(player, "You have been teleported to the survival world.");
        return;
    }
    
    /**
     * Immediately teleports the player back to the spawn location of their current world. This
     * location may be updated by staff members using the "/world set spawn" command.
     * 
     * @param sender    The player who would like to teleport to the spawn position.
     * @param arguments Additional arguments to the /spawn command - not used.
     */
    @CommandHandler("spawn")
    public void onSpawnCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        if (!player.hasPermission("command.spawn")) {
            displayCommandError(player, "You don't have permission to use the /spawn command.");
            return;
        }
        
        PlayerLog.record(RecordType.SPAWN_TELEPORTED, getUserId(player));
        
        displayCommandSuccess(player, "You have been teleported back to the spawn!");
        player.teleport(player.getWorld().getSpawnLocation());
    }
    
    /**
     * Provides auto-completion suggestions for the /world command. These can be activated by
     * players typing partial commands and then using <tab> to complete a suggestion.
     * 
     * @param sender    The player is planning on executing the /world command.
     * @param arguments The arguments which they've entered so far.
     * @return          A list of suggestions, or NULL.
     */
    @CommandCompletionHandler("world")
    public List<String> onWorldCompletion(CommandSender sender, String[] arguments) {
        if (arguments.length >= 2 && arguments[0].equals("list"))
            return null; // no auto-completions for /world list.
        
        List<String> suggestions = new ArrayList<String>();
        if (arguments.length >= 2 && arguments[0].equals("set")) {
            if (arguments.length >= 3 && arguments[1].equals("rule")) {
                for (String option : mGameRulesMap.keySet()) {
                    if (!option.startsWith(arguments[2]))
                        continue;
                    
                    suggestions.add(option);
                }
                
                Collections.sort(suggestions);
                return suggestions;
            }
            
            for (String option : Arrays.asList("animals", "difficulty", "mobs", "pvp", "readonly", "rule", "spawn")) {
                if (!option.startsWith(arguments[1]))
                    continue;
                
                suggestions.add(option);
            }
            
            return suggestions;
        }
        
        if (arguments.length >= 2 && Arrays.asList("classic", "creative", "destroy", "survival", "warp").contains(arguments[0])) {
            for (World world : getServer().getWorlds()) {
                final String name = world.getName();
                if (name.startsWith(arguments[1]))
                    suggestions.add(name);
            }
            
            Collections.sort(suggestions);
            return suggestions;
        }
        
        for (String option : Arrays.asList("classic", "create", "creative", "destroy", "list", "set", "survival", "warp")) {
            if (option.startsWith(arguments[0]))
                suggestions.add(option);
        }

        return suggestions;
    }
    
    /**
     * The /world command is the primary entry point for Management members to manipulate the worlds
     * available on Mineground. New worlds can be created, current worlds can be renamed and removed
     * and settings of rules can be adjusted at their discretion.
     * 
     * /world                   Displays usage information for the /world command.
     * /world classic           Displays or changes the classic world on Mineground.
     * /world create            Creates a new world on the server running Mineground.
     * /world creative          Displays or changes the creative world on Mineground.
     * /world default           Displays or changes the default world on Mineground.
     * /world destroy           Destroys one of the world currently existing on Mineground.
     * /world list              Lists the existing worlds on Mineground.
     * /world set               Lists options which can be set for the current world.
     * /world set animals       Changes how many animals should spawn per chunk in this world.
     * /world set difficulty    Changes the difficulty of this world.
     * /world set mobs          Changes how many mobs should spawn per chunk in this world.
     * /world set pvp           Changes whether player-versus-player is allowed in this world.
     * /world set readonly      Changes whether this world should be made read-only.
     * /world set rule          Changes various advanced game rule values exposed by Minecraft.
     * /world set spawn         Changes the spawn position. A value is necessary to change it.
     * /world warp              Warps to the spawn position in another world.
     * 
     * For each option in "/world set" the rule is that it will display the value of the setting,
     * unless a fourth argument has been passed with the new value.
     * 
     * @param sender    The player who executed this command.
     * @param arguments The arguments which they passed on whilst executing.
     */
    @CommandHandler("world")
    public void onWorldCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        final Account account = getAccountForPlayer(player);

        if (account == null || !player.hasPermission("world.list")) {
            displayCommandError(player, "You don't have permission to execute this command.");
            return;
        }
        
        // Displays or changes the world which is known on Mineground as the "classic" world.
        // Usually this is the previous map used on the server. Players can then use the /classic
        // command to visit this. It can be made read-only by administrators.
        if (arguments.length >= 1 && arguments[0].equals("classic")) {
            if (!player.hasPermission("world.classic")) {
                displayCommandError(player, "You don't have permission to change the classic world.");
                return;
            }
            
            if (arguments.length == 1) {
                final World classicWorld = getFeature().getClassicWorld();
                if (classicWorld == null) {
                    displayCommandError(player, "No classic world has been defined for Mineground.");
                    displayCommandUsage(player, "/world classic [name]");
                    return;
                }
                
                displayCommandSuccess(player, "**" + classicWorld.getName() + "** is currently defined as the classic world.");
                return;
            }
            
            final World newClassicWorld = getServer().getWorld(arguments[1]);
            if (newClassicWorld == null) {
                displayCommandError(player, "The world **" + arguments[1] + "** does not exist on Mineground.");
                return;
            }
            
            // TODO: Inform administrators about this change.
            
            getFeature().setClassicWorld(newClassicWorld);

            displayCommandSuccess(player, "The classic world has been updated to **" + newClassicWorld.getName() + "**.");
            return;
        }
        
        // Creates a new world on Mineground. Because of the sensitivity of this command, it is only
        // available to server operators (i.e. Management members). Players won't be able to get to
        // this world, unless it's exposed as the new creative, classic or survival world.
        if (arguments.length >= 1 && arguments[0].equals("create")) {
            if (!player.isOp() || !player.hasPermission("world.create")) {
                displayCommandError(player, "You need to be a Server Operator in order to create new worlds.");
                return;
            }
            
            // The world create command has 4 arguments: "create", [type], [environment], [name]
            if (arguments.length < 4) {
                displayCommandUsage(player, "/world create [amplified/biomes/flat/normal] [end/nether/normal] [name]");
                return;
            }
            
            WorldType worldType = null;
            if (arguments[1].equalsIgnoreCase("amplified"))
                worldType = WorldType.AMPLIFIED;
            else if (arguments[1].equalsIgnoreCase("biomes"))
                worldType = WorldType.LARGE_BIOMES;
            else if (arguments[1].equalsIgnoreCase("flat"))
                worldType = WorldType.FLAT;
            else if (arguments[1].equalsIgnoreCase("normal"))
                worldType = WorldType.NORMAL;
            else {
                displayCommandError(player, "The world type **" + arguments[1] + "** is not a valid type.");
                return;
            }
            
            Environment environment = null;
            if (arguments[2].equalsIgnoreCase("end"))
                environment = Environment.THE_END;
            else if (arguments[2].equalsIgnoreCase("nether"))
                environment = Environment.NETHER;
            else if (arguments[2].equalsIgnoreCase("normal"))
                environment = Environment.NORMAL;
            else {
                displayCommandError(player, "The value **" + arguments[2] + "** is not a valid environment.");
                return;
            }
            
            final String name = arguments[3];
            if (getServer().getWorld(name) != null) {
                displayCommandError(player, "There already is a world named **" + name + "**.");
                return;
            }
            
            // TODO: Inform administrators of this action taking place.

            // Announce to all online players that a new world is being created, as this will stop
            // their commands and events for the duration of the creation.
            mCreatingWorldMessage.setString("nickname", player.getName());
            mCreatingWorldMessage.send(getServer().getOnlinePlayers(), Color.PLAYER_EVENT);
            
            WorldCreator creator = new WorldCreator(name);
            creator.environment(environment);
            creator.generateStructures(true);
            creator.seed(UUID.randomUUID().getMostSignificantBits());
            creator.type(worldType);
            
            // Tell Bukkit to create the world. This takes an awkwardly long time.
            getServer().createWorld(creator);
            
            // Announce that the world has been created, and that players can continue playing.
            mWorldCreatedMessage.setString("nickname", player.getName());
            mWorldCreatedMessage.send(getServer().getOnlinePlayers(), Color.PLAYER_EVENT);
            
            displayCommandSuccess(player, "The world **" + name + "** has successfully been created!");
            return;
        }
        
        // The creative world  is a world in which all players can build whatever they like, with
        // almost no restrictions on the blocks they're able to get. It is accessible using the
        // /creative command, and Management members can change it using the /world create command.
        if (arguments.length >= 1 && arguments[0].equals("creative")) {
            if (!player.hasPermission("world.creative")) {
                displayCommandError(player, "You don't have permission to change the creative world.");
                return;
            }
            
            if (arguments.length == 1) {
                final World creativeWorld = getFeature().getCreativeWorld();
                if (creativeWorld == null) {
                    displayCommandError(player, "No creative world has been defined for Mineground.");
                    displayCommandUsage(player, "/world creative [name]");
                    return;
                }
                
                displayCommandSuccess(player, "**" + creativeWorld.getName() + "** is currently defined as the creative world.");
                return;
            }
            
            final World newCreativeWorld = getServer().getWorld(arguments[1]);
            if (newCreativeWorld == null) {
                displayCommandError(player, "The world **" + arguments[1] + "** does not exist on Mineground.");
                return;
            }
            
            // TODO: Inform administrators about this change.
            
            getFeature().setCreativeWorld(newCreativeWorld);

            displayCommandSuccess(player, "The creative world has been updated to **" + newCreativeWorld.getName() + "**.");
            return;
        }
        
        // Destroys a world on Mineground. Worlds destroyed using this command cannot be brought
        // back, unless the files on the server were backed up to another directory or location.
        // This is an extremely sensitive command that shouldn't be played around with.
        if (arguments.length >= 1 && arguments[0].equals("destroy")) {
            if (!player.isOp() || !player.hasPermission("world.destroy")) {
                displayCommandError(player, "You need to be a Server Operator in order to destroy worlds.");
                return;
            }
            
            // The word "CONFIRMED" needs to follow the world's name, to make this command a bit
            // awkward to use. It mustn't be too easy to accidentally remove a world.
            if (arguments.length != 3 || !arguments[2].equals("CONFIRMED")) {
                displayCommandUsage(player, "/world destroy [name] CONFIRMED");
                return;
            }
            
            final World world = getServer().getWorld(arguments[1]);
            if (world == null) {
                displayCommandError(player, "The world **" + arguments[1] + "** does not exist on Mineground.");
                return;
            }
            
            if (world == getFeature().getDefaultWorld()) {
                displayCommandError(player, "You cannot remove the default world.");
                return;
            }
            
            // TODO: Announce this action to other administrators.

            // First teleport all players who currently are in this world out of it.
            Location defaultSpawn = getFeature().getDefaultWorld().getSpawnLocation();
            
            mWorldRemovedTeleportMessage.setString("nickname", player.getName());
            mWorldRemovedTeleportMessage.send(world.getPlayers(), Color.PLAYER_EVENT);
            
            for (Player p : world.getPlayers())
                p.teleport(defaultSpawn);
            
            // Now that everyone is out of the world, remove it. Be sure to save the latest state.
            if (!getServer().unloadWorld(world, true)) {
                displayCommandError(player, "An unknown Bukkit error occurred while removing the world.");
            } else {
                getFeature().onRemoveWorld(world);
            }
            
            if (getFeature().getCreativeWorld() == world)
                getFeature().setCreativeWorld(null);
            
            if (getFeature().getClassicWorld() == world)
                getFeature().setClassicWorld(null);
            
            displayCommandSuccess(player, "The world **" + world.getName() + "** has been removed.");
            return;
        }

        // Displays a list of the worlds created on Mineground.
        if (arguments.length >= 1 && arguments[0].equals("list")) {
            displayCommandSuccess(player, "The following worlds are available on Mineground:");
            for (World world : getServer().getWorlds()) {
                String annotation = "";
                if (getFeature().getClassicWorld() == world)
                    annotation += "§cclassic§7, ";
                if (getFeature().getCreativeWorld() == world)
                    annotation += "§ccreative§7, ";
                if (getFeature().getDefaultWorld() == world)
                    annotation += "§csurvival§7, ";
                
                String environment = "[unknown]";
                if (world.getEnvironment() == Environment.THE_END)
                    environment = "The End";
                else if (world.getEnvironment() == Environment.NETHER)
                    environment = "Nether";
                else if (world.getEnvironment() == Environment.NORMAL)
                    environment = "Normal";
                
                displayCommandDescription(player, "  " + world.getName() + " §7(" + annotation + environment + ")");
            }

            return;
        }
        
        // The /world set command exposes a wide variety of functions available to change settings
        // of the world the player is currently in. While more basic features such as the time and
        // weather can be control by more players, these are the more powerful settings.
        if (arguments.length >= 1 && arguments[0].equals("set")) {
            final World world = player.getWorld();

            // Changes the number of animals which should be spawned per chunk in the current world.
            // When there is a larger number of players in-game, this could significantly stress the
            // server's CPU, so it should be kept within reasonable limits.
            if (arguments.length >= 2 && arguments[1].equals("animals")) {
                if (!player.hasPermission("world.set.animals")) {
                    displayCommandError(player, "You don't have permission to change the animal spawn count.");
                    return;
                }
                
                if (arguments.length >= 3) {
                    int value = -1;
                    try {
                        int inputValue = Integer.parseInt(arguments[2]);
                        if (inputValue >= 0 && inputValue <= ENTITY_SPAWN_LIMIT)
                            value = inputValue;
                        
                    } catch (NumberFormatException exception) { }
                    
                    // TODO: Announce to administrators that the animal spawn limit has changed.
                    
                    displayCommandSuccess(player, "The animal spawn limit has been changed to " + value + "!");
                    world.setAnimalSpawnLimit(value);
                    return;
                }
                
                displayCommandDescription(player, "The per-chunk animal spawn limit for this world is: §2" + world.getAnimalSpawnLimit());
                displayCommandDescription(player, "Change this using §b/world set animals [default, 0-" + ENTITY_SPAWN_LIMIT + "]§f.");
                return;
            }

            // Changing the difficulty level of a Minecraft world determines what kind of mobs will
            // spawn, how much damage they will do and whether hunger can kill the player.
            if (arguments.length >= 2 && arguments[1].equals("difficulty")) {
                if (!player.hasPermission("world.set.difficulty")) {
                    displayCommandError(player, "You don't have permission to change the world's difficulty.");
                    return;
                }
                
                if (arguments.length >= 3) {
                    Difficulty difficulty = Difficulty.PEACEFUL;
                    if (arguments[2].equals("easy"))
                        difficulty = Difficulty.EASY;
                    else if (arguments[2].equals("normal"))
                        difficulty = Difficulty.NORMAL;
                    else if (arguments[2].equals("hard"))
                        difficulty = Difficulty.HARD;
                    
                    // TODO: Announce to administrators that the difficulty level has changed.
                    
                    displayCommandSuccess(player, "The world's difficulty level has been changed!");
                    world.setDifficulty(difficulty);
                    return;
                }
                
                String value = "unknown";
                switch (world.getDifficulty()) {
                    case PEACEFUL:
                        value = "peaceful";
                        break;
                    case EASY:
                        value = "easy";
                        break;
                    case NORMAL:
                        value = "normal";
                        break;
                    case HARD:
                        value = "hard";
                        break;
                }
                
                displayCommandDescription(player, "The difficulty level for this world is: §2" + value);
                displayCommandDescription(player, "Change this using §b/world set difficulty [peaceful, easy, normal, hard]§f.");
                return;
            }
            
            // Changes how many mobs can spawn in a chunk at any given moment. Like with the animals
            // setting, the maximum value of this setting is capped by |ENTITY_SPAWN_LIMIT|.
            if (arguments.length >= 2 && arguments[1].equals("mobs")) {
                if (!player.hasPermission("world.set.mobs")) {
                    displayCommandError(player, "You don't have permission to change the mob spawn count.");
                    return;
                }
                
                if (arguments.length >= 3) {
                    int value = -1;
                    try {
                        int inputValue = Integer.parseInt(arguments[2]);
                        if (inputValue >= 0 && inputValue <= ENTITY_SPAWN_LIMIT)
                            value = inputValue;
                        
                    } catch (NumberFormatException exception) { }
                    
                    // TODO: Announce to administrators that the mob spawn limit has changed.
                    
                    displayCommandSuccess(player, "The mob spawn limit has been changed to " + value + "!");
                    world.setMonsterSpawnLimit(value);
                    return;
                }
                
                displayCommandDescription(player, "The per-chunk mob spawn limit for this world is: §2" + world.getMonsterSpawnLimit());
                displayCommandDescription(player, "Change this using §b/world set mobs [default, 0-" + ENTITY_SPAWN_LIMIT + "]§f.");
                return;
            }
            
            // The /world set pvp command allows staff to change whether player-versus-player (PVP)
            // fighting should be allowed. There are three options available: "default", "allowed"
            // and "disallowed". The first option allows users to set it for themselves using /pvp.
            // "allowed" always allows it; "disallowed" always disabled it, regardless of settings.
            if (arguments.length >= 2 && arguments[1].equals("pvp")) {
                if (!player.hasPermission("world.set.pvp")) {
                    displayCommandError(player, "You don't have permission to change whether PVP is allowed.");
                    return;
                }
                
                if (arguments.length >= 3) {
                    PvpSetting value = null;
                    if (arguments[2].equals("allowed"))
                        value = PvpSetting.PVP_ALLOWED;
                    else if (arguments[2].equals("disallowed"))
                        value = PvpSetting.PVP_DISALLOWED;
                    else
                        value = PvpSetting.PVP_DEFAULT;
                    
                    // TODO: Announce to in-game staff that the spawn position has been changed.
                    
                    displayCommandSuccess(player, "The PVP settings for this world have been updated!");
                    getFeature().getWorldSettings(world).setPvp(value);
                    return;
                }
                
                String value = "unknown";
                switch (getFeature().getWorldSettings(world).getPvp()) {
                    case PVP_ALLOWED:
                        value = "allowed";
                        break;
                    case PVP_DISALLOWED:
                        value = "disallowed";
                        break;
                    default:
                        value = "default";
                        break;
                }
                
                displayCommandDescription(player, "The PVP setting for this world is: §2" + value);
                displayCommandDescription(player, "Change this using §b/world set pvp [allowed, disallowed, default]§f.");
                return;
            }
            
            // We have implemented the ability to make entire worlds read-only, support for which
            // can be toggled using this command. It affects whether people can modify the blocks.
            if (arguments.length >= 2 && arguments[1].equals("readonly")) {
                if (!player.hasPermission("world.set.readonly")) {
                    displayCommandError(player, "You don't have permission to make a world read-only.");
                    return;
                }
                
                if (arguments.length >= 3) {
                    boolean frozen = argumentAsBoolean(arguments[2]);
                    getFeature().getWorldSettings(world).setReadOnly(frozen);
                    
                    // TODO: Announce this change to administrators.
                    
                    displayCommandSuccess(player, "The current world has successfully been **" + (frozen ? "" : "un") + "frozen**.");
                    return;
                }
                
                boolean mutable = !getFeature().getWorldSettings(world).isReadOnly();
                
                displayCommandDescription(player, "This world is currently defined as §2" + (mutable ? "mutable" : "read-only"));
                displayCommandDescription(player, "Change this using §b/world set readonly [true, false]§f.");
                return;
            }
            
            // Bukkit has a system called "game rules" which sets various additional options for
            // the world. We implement these in a single option, "rule", followed by a brief text
            // representing the setting which should be changed.
            if (arguments.length >= 2 && arguments[1].equals("rule")) {
                if (!player.hasPermission("world.set.rule")) {
                    displayCommandError(player, "You don't have permission to change the world's game rules.");
                    return;
                }
                
                String gameRule = null;
                if (arguments.length >= 3)
                    gameRule = mGameRulesMap.get(arguments[2]);
                    
                if (gameRule == null) {
                    String gameRulesList = "";
                    for (String rule : mGameRulesMap.keySet())
                        gameRulesList += "§e" + rule + "§f, ";
                    
                    displayCommandDescription(player, "Available game rules: " + gameRulesList);
                    return;
                }
               
                // We found the game rule which should be changed. If the player supplied a fourth
                // argument (either "true" or "false"), change the value, otherwise display it.
                if (arguments.length >= 4 && (arguments[3].equals("true") || arguments[3].equals("false"))) {
                    // TODO: Announce to in-game staff that the spawn position has been changed.
                    
                    displayCommandSuccess(player, "The game rule has been updated for this world!");
                    world.setGameRuleValue(gameRule, arguments[3]);
                    return;
                }
                
                displayCommandDescription(player, "The game rule §2" + arguments[2] + "§f is set to: §2" + world.getGameRuleValue(gameRule));
                displayCommandDescription(player, "Change this using §b/world set rule " + arguments[2] + " [true, false]§f.");
                return;
            }
            
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

                displayCommandDescription(player, "Change this using §b/world set spawn here§f.");
                return;
            }
            
            // If no recognized sub-command for /world set has been passed, show them general usage
            // information, which includes a list of the available sub-commands.
            displayCommandUsage(player, "/world set [animals/difficulty/mobs/pvp/rule/spawn]");
            displayCommandDescription(player, "Changes various settings related to worlds on Mineground.");
            return;
        }
        
        // The survival world is the default world. It's accessible using the /survival command if
        // the player is in another world. This command updates which world that is.
        if (arguments.length >= 1 && arguments[0].equals("survival")) {
            if (!player.hasPermission("world.survival")) {
                displayCommandError(player, "You don't have permission to change the survival world.");
                return;
            }
            
            if (arguments.length == 1) {
                final World survivalWorld = getFeature().getCreativeWorld();
                if (survivalWorld == null) {
                    displayCommandError(player, "No survival world has been defined for Mineground.");
                    displayCommandUsage(player, "/world survival [name]");
                    return;
                }
                
                displayCommandSuccess(player, "**" + survivalWorld.getName() + "** is currently defined as the survival world.");
                return;
            }
            
            final World newSurvivalWorld = getServer().getWorld(arguments[1]);
            if (newSurvivalWorld == null) {
                displayCommandError(player, "The world **" + arguments[1] + "** does not exist on Mineground.");
                return;
            }
            
            // TODO: Inform administrators about this change.
            
            getFeature().setDefaultWorld(newSurvivalWorld);

            displayCommandSuccess(player, "The survival world has been updated to **" + newSurvivalWorld.getName() + "**.");
            return;
        }
        
        // Warps the player to the spawn position in another world. This is useful when changing the
        // worlds the /creative, /classic and /survival commands map to, since changing them
        // requires you to be in those worlds. Only administrators can warp to all worlds.
        if (arguments.length >= 1 && arguments[0].equals("warp")) {
            if (!player.hasPermission("world.warp")) {
                displayCommandError(player, "You do not have permission to warp to other worlds.");
                return;
            }
            
            if (arguments.length == 1) {
                displayCommandUsage(player, "/world warp [name]");
                return;
            }
            
            final World target = getServer().getWorld(arguments[1]);
            if (target == null) {
                displayCommandError(player, "The world **" + arguments[1] + "** does not exist on Mineground.");
                return;
            }

            player.teleport(target.getSpawnLocation());

            displayCommandSuccess(player, "You have been teleported to world **" + arguments[1] + "**.");
            return;
        }
        
        // If no valid command for /world has been passed, show them general usage information. This
        // also displays all the individual /world options available.
        displayCommandUsage(player, "/world [classic/create/creative/destroy/list/set/survival/warp]");
        displayCommandDescription(player, "Creates and manages the worlds available on Mineground.");
    }
}
