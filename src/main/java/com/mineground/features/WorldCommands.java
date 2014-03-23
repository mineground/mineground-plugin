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
import com.mineground.base.CommandCompletionHandler;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureComponent;
import com.mineground.base.FeatureInitParams;
import com.mineground.features.WorldManager.PvpSetting;

/**
 * Implementations of the commands associated with the world manager. These grant certain players
 * and staff members access to various options in regards to Minecraft worlds.
 */
public class WorldCommands extends FeatureComponent<WorldManager> {
    // The maximum number of entities which may be spawned per chunk in a world.
    private final static int ENTITY_SPAWN_LIMIT = 150;
    
    // Map containing the game rule mappings supported by Mineground.
    private final Map<String, String> mGameRulesMap;
    
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
            
            for (String option : Arrays.asList("animals", "difficulty", "mobs", "pvp", "rule", "spawn")) {
                if (!option.startsWith(arguments[1]))
                    continue;
                
                suggestions.add(option);
            }
            
            return suggestions;
        }
        
        if (arguments.length >= 2 && Arrays.asList("create", "destroy").contains(arguments[0])) {
            for (World world : getServer().getWorlds())
                suggestions.add(world.getName());
            
            Collections.sort(suggestions);
            return suggestions;
        }

        if ("create".startsWith(arguments[0]))
            suggestions.add("create");
        if ("destroy".startsWith(arguments[0]))
            suggestions.add("destroy");
        if ("list".startsWith(arguments[0]))
            suggestions.add("list");
        if ("set".startsWith(arguments[0]))
            suggestions.add("set");

        return suggestions;
    }
    
    /**
     * The /world command is the primary entry point for Management members to manipulate the worlds
     * available on Mineground. New worlds can be created, current worlds can be renamed and removed
     * and settings of rules can be adjusted at their discretion.
     * 
     * /world                   Displays usage information for the /world command.
     * /world create            Creates a new world on the server running Mineground.
     * /world destroy           Destroys one of the world currently existing on Mineground.
     * /world list              Lists the existing worlds on Mineground.
     * /world set               Lists options which can be set for the current world.
     * /world set animals       Changes how many animals should spawn per chunk in this world.
     * /world set difficulty    Changes the difficulty of this world.
     * /world set mobs          Changes how many mobs should spawn per chunk in this world.
     * /world set pvp           Changes whether player-versus-player is allowed in this world.
     * /world set rule          Changes various advanced game rule values exposed by Minecraft.
     * /world set spawn         Changes the spawn position. A value is necessary to change it.
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
        
        // Creates a new world on Mineground. Because of the sensitivity of this command, it is only
        // available to server operators (i.e. Management members). Players won't be able to get to
        // this world, unless it's exposed as the new creative, classic or survival world.
        if (arguments.length >= 1 && arguments[0].equals("create")) {
            if (!player.isOp()) {
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
            // TODO: Announce creation of the world to all players.
            
            WorldCreator creator = new WorldCreator(name);
            creator.environment(environment);
            creator.generateStructures(true);
            creator.seed(UUID.randomUUID().getMostSignificantBits());
            creator.type(worldType);
            
            getServer().createWorld(creator);
            
            // TODO: Announce that the world has been created.
            
            return;
        }
        
        // Destroys a world on Mineground. Worlds destroyed using this command cannot be brought
        // back, unless the files on the server were backed up to another directory or location.
        // This is an extremely sensitive command that shouldn't be played around with.
        if (arguments.length >= 1 && arguments[0].equals("destroy")) {
            if (!player.isOp()) {
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
            
            // TODO: Remove the world.
            
            return;
        }

        // Displays a list of the worlds created on Mineground.
        if (arguments.length >= 1 && arguments[0].equals("list")) {
            displayCommandSuccess(player, "The following worlds are available on Mineground:");
            for (World w : getServer().getWorlds())
                displayCommandDescription(player, "  " + w.getName());

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
                    getFeature().setPlayerVersusPlayer(world, value);
                    return;
                }
                
                String value = "unknown";
                switch (getFeature().getPlayerVersusPlayer(world)) {
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
        
        // If no valid command for /world has been passed, show them general usage information. This
        // also displays all the individual /world options available.
        displayCommandUsage(player, "/world [create/destroy/list/set]");
        displayCommandDescription(player, "Creates and manages the worlds available on Mineground.");
    }
}
