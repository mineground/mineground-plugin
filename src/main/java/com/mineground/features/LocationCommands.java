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
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.account.Account;
import com.mineground.account.PlayerLog;
import com.mineground.account.PlayerLog.RecordType;
import com.mineground.base.CommandCompletionHandler;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureComponent;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
import com.mineground.base.SimpleHash;

public class LocationCommands extends FeatureComponent<LocationManager> {
    /**
     * Maximum number of characters on a single line when listing a player's warps using the "/warp
     * list" command. We should try to avoid wrapping here.
     */
    private static final int MAX_WARP_LIST_LINE_LENGTH = 50;
    
    public LocationCommands(LocationManager manager, FeatureInitParams params) {
        super(manager, params);
    }
    
    /**
     * Implements the /home command, which is a convenient way for players to teleport back to their
     * homes. It does not matter in which world the player currently is for this command to work.
     * The player can update their home location at all times as well.
     * 
     * /home        Teleports to the stored player's home location.
     * /home set    Updates the player's home location to their current position.
     * 
     * @param sender    The Player or console who executed this command.
     * @param arguments The arguments which they passed on to this command.
     */
    @CommandHandler("home")
    public void onHomeCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        final Account account = getAccountForPlayer(player);
        
        if (account == null)
            return; // account issues cannot be resolved by this command.
        
        // Players are able to update their home location by typing "/home set", which will create
        // a new (hidden) warp location at their exact position, and save it so that is persists.
        if (arguments.length > 0 && arguments[0].equals("set")) {
            if (!player.hasPermission("warp.create")) {
                displayCommandError(player, "You are not allowed to store your home location.");
                return;
            }

            final Location location = player.getLocation();
            getFeature().createLocation(player, location, "", "").then(new PromiseResultHandler<Integer>() {
                public void onFulfilled(Integer locationId) {
                    // Records that the player created a new warp with Id |locationId|.
                    PlayerLog.record(RecordType.HOME_CREATED, getUserId(player), locationId);

                    displayCommandSuccess(player, "Your home location has been updated!");
                    account.setHomeLocation(locationId);
                }
                public void onRejected(PromiseError error) {
                    displayCommandError(player, error.reason());
                }
            });
            
            return;
        }
        
        int locationId = account.getHomeLocation();
        if (locationId == 0) {
            displayCommandError(player, "You haven't saved your home location yet!");
            displayCommandDescription(player, "Type \"/home set\" to update the location at any time.");
            return;
        }
        
        getFeature().findLocationById(locationId).then(new PromiseResultHandler<LocationRecord>() {
            public void onFulfilled(LocationRecord location) {
                Location destination = location.toBukkitLocation(getServer().getWorlds());
                if (destination == null) {
                    displayCommandError(player, "Your home location isn't in a valid world anymore!");
                    displayCommandDescription(player, "Type \"/home set\" to update the location at any time.");
                    return;
                }
                
                // Records that the player has warped to their home location.
                PlayerLog.record(RecordType.HOME_TELEPORTED, getUserId(player), location.location_id);
                
                displayCommandSuccess(player, "Welcome home, " + player.getName() + "!");
                player.teleport(destination);
            }
            public void onRejected(PromiseError error) {
                displayCommandError(player, "Your home location couldn't be loaded..");
                displayCommandDescription(player, "Type \"/home set\" to update the location at any time.");
            }
        });
    }
    
    /**
     * Provides auto-completion suggestions for the /warp command. This will make it significantly
     * easier for players to work with their warps, as they can use <tab> to complete long names.
     * 
     * @param sender    The Player or console who executed this command.
     * @param arguments The arguments which they passed on to this command.
     * @return          A list of auto-completion suggestions based on their current input.
     */
    @CommandCompletionHandler("warp")
    public List<String> onWarpCompletion(CommandSender sender, String[] arguments) {
        if (arguments.length >= 2 && arguments[0].equals("list"))
            return null; // no auto-completions for /warp list.
        if (arguments.length >= 2 && arguments[0].equals("create"))
            return null; // no auto-completions for /warp create.
 
        List<String> suggestions = new ArrayList<String>();
        String completionWord = "";
        
        if (arguments.length == 1) {
            completionWord = arguments[0];
            if ("create".startsWith(completionWord))
                suggestions.add("create");
            if ("list".startsWith(completionWord))
                suggestions.add("list");
            if ("remove".startsWith(completionWord))
                suggestions.add("remove");
            
        } else if (arguments.length >= 2)
            completionWord = arguments[1];
        
        // TODO: Suggest warp names based on |completionWord|.

        Collections.sort(suggestions);
        return suggestions;
    }
    
    /**
     * Implements the /warp command, which is the primary interface for players to manage their
     * stored locations with. This is a reasonably complicated command with the following options:
     * 
     * /warp                    Displays usage information for the command.
     * /warp create             Displays usage information for creating warps.
     * /warp create NAME        Creates warp |NAME| in the current world.
     * /warp create NAME PASS   Creates warp |NAME| in the current world, protected by |PASS|.
     * /warp list               Lists all warps created by the player in the current world.
     * /warp remove             Displays usage information for removing warps.
     * /warp remove NAME        Removes warp |NAME| from the current world.
     * /warp NAME               Teleports to the warp |NAME| in the current world.
     * /warp NAME PASS          Teleports to the warp |NAME| in the current world, using |PASS|.
     * 
     * The functionality exposed by this command is protected by a set of permissions, all defined
     * and granted to the various player levels in the plugin.yml file of Mineground.
     * 
     * @param sender    The Player or console who executed this command.
     * @param arguments The arguments which they passed on to this command.
     */
    @CommandHandler("warp")
    public void onWarpCommand(CommandSender sender, String[] arguments) {
        if (arguments.length == 0) {
            displayCommandUsage(sender, "/warp [create/list/remove/§nname§r]");
            displayCommandDescription(sender, "Creates, removes, lists or teleports to your locations.");
            return;
        }
        
        final Player player = (Player) sender;
        final World world = player.getWorld();
        
        // Players may have the ability to create new warp locations by using the /warp create
        // command. The locations will persist in the database.
        if (arguments[0].equals("create")) {
            if (!player.hasPermission("warp.create")) {
                displayCommandError(player, "You are not allowed to create new warp locations.");
                return;
            }
            
            if (arguments.length == 1) {
                displayCommandUsage(player, "/warp create [name] §7[password]");
                displayCommandDescription(player, "Saves a location, optionally protected with a password.");
                return;
            }
            
            final String locationName = arguments[1];
            final String password = (arguments.length >= 3) ? arguments[2] : "";
            
            // Note that we need to store the player's location here because findLocation() will
            // be resolved asynchronously, at which time the player may have moved.
            final Location location = player.getLocation();
            
            getFeature().findLocation(locationName, world).then(new PromiseResultHandler<LocationRecord>() {
                public void onFulfilled(LocationRecord result) {
                    displayCommandError(player, "A warp named \"" + locationName + "\" already exists in this world.");
                }
                public void onRejected(PromiseError error) {
                    // It may seem odd to do the work in onRejected(), but in case of findLocation
                    // it means that the location was *not* found, which is what we want when the
                    // player tries to create a new location in a certain world.
                    getFeature().createLocation(player, location, locationName, password).then(new PromiseResultHandler<Integer>() {
                        public void onFulfilled(Integer locationId) {
                            // Records that the player created a new warp with Id |locationId|.
                            PlayerLog.record(RecordType.WARP_CREATED, getUserId(player), locationId);

                            displayCommandSuccess(player, "The warp \"" + locationName + "\" has been created!");
                        }
                        public void onRejected(PromiseError error) {
                            displayCommandError(player, error.reason());
                        }
                    });
                }
            });

            return;
        }

        // Lists the warps created by the player, with up to ten warps per line. This can get quite
        // spammy when the player has created a ton of warps, but then it's up to them to clean it
        // up. Only warps in the player's current world will be returned.
        if (arguments[0].equals("list")) {
            getFeature().listLocations(player, world).then(new PromiseResultHandler<List<String>>() {
                public void onFulfilled(List<String> locations) {
                    displayCommandSuccess(player, "You currently have " + locations.size() + " saved locations!");

                    StringBuilder messageBuilder = new StringBuilder();
                    for (String location : locations) {
                        messageBuilder.append(location);
                        if (messageBuilder.length() >= MAX_WARP_LIST_LINE_LENGTH) {
                            player.sendMessage(messageBuilder.toString());
                            messageBuilder.setLength(0);
                        } else
                            messageBuilder.append(", ");
                    }
                    
                    int length = messageBuilder.length();
                    if (length == 0)
                        return;
                    
                    player.sendMessage(messageBuilder.delete(length - 2, length).toString());
                }
                public void onRejected(PromiseError error) {
                    displayCommandError(player, error.reason());
                }
            });
            
            return;
        }
        
        // When a warp is no longer necessary, or a player just wants to clean up the list of warps
        // they're maintaining, the /warp remove command is just the right tool for them. Staff
        // members have the ability to remove any warp at any time.
        if (arguments[0].equals("remove")) {
            if (!player.hasPermission("warp.remove")) {
                displayCommandError(player, "You are not allowed to remove saved locations.");
                return;
            }
            
            if (arguments.length == 1) {
                displayCommandUsage(player, "/warp remove [name]");
                displayCommandDescription(player, "Allows you to remove a saved location from this world.");
                return;
            }
            
            final String locationName = arguments[1];
            
            getFeature().findLocation(locationName, world).then(new PromiseResultHandler<LocationRecord>() {
                public void onFulfilled(final LocationRecord location) {
                    // Players normally are only allowed to remove their own warps, but members of
                    // Mineground's staff will be allowed to remove any warp from the world.
                    if (location.user_id != getUserId(player) && !player.hasPermission("warp.remove_all")) {
                        displayCommandError(player, "You can only remove your own saved locations!");
                        return;
                    }
                    
                    getFeature().removeLocation(location).then(new PromiseResultHandler<Void>() {
                        public void onFulfilled(Void result) {
                            // Records that the player has removed the saved location |location|.
                            PlayerLog.record(RecordType.WARP_REMOVED, getUserId(player), location.location_id);
                            
                            displayCommandSuccess(player, "The location \"" + locationName + "\" has been removed from this world.");
                        }
                        public void onRejected(PromiseError error) {
                            displayCommandError(player, "The location could not be removed because of a database error.");
                        }
                    });
                }
                public void onRejected(PromiseError error) {
                    displayCommandError(player, "The location \"" + locationName + "\" does not exist in this world.");
                }
            });
            
            return;
        }
        
        // The player wants to teleport to a previously created warp in the current world. Find the
        // warp in the database, make sure that the password matches, and then teleport them.
        final String destination = arguments[0];
        final String password = (arguments.length >= 2) ? arguments[1] : "";
        
        if (!player.hasPermission("warp.teleport")) {
            displayCommandError(player, "You are not allowed to teleport to saved locations.");
            return;
        }

        getFeature().findLocation(destination, world).then(new PromiseResultHandler<LocationRecord>() {
            public void onFulfilled(LocationRecord location) {
                // If the location has been protected by a password, this needs to be verified. Only
                // players with the warp.teleport_no_password permission are able to override this.
                if (location.password != 0 && !player.hasPermission("warp.teleport_no_password")) {
                    if (SimpleHash.createHash(password) != location.password) {
                        displayCommandError(player, "You need to enter the correct password for the location \"" + destination + "\".");
                        return;
                    }
                }
                
                // Records that the player teleported to |location|.
                PlayerLog.record(RecordType.WARP_TELEPORTED, getUserId(player), location.location_id);
                
                displayCommandSuccess(player, "You have been teleported to " + destination + "!");
                player.teleport(location.toBukkitLocation(getServer().getWorlds()));
            }
            public void onRejected(PromiseError error) {
                displayCommandError(player, "The location \"" + destination + "\" does not exist in this world.");
            }
        });
    }
}
