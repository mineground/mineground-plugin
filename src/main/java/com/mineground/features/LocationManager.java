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
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.account.Account;
import com.mineground.account.PlayerLog;
import com.mineground.account.PlayerLog.RecordType;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Promise;
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
import com.mineground.base.SimpleHash;
import com.mineground.database.DatabaseResult;
import com.mineground.database.DatabaseResultRow;
import com.mineground.database.DatabaseStatement;

/**
 * The Warp Manager owns the /warp command, which is one of Mineground's most popular commands as it
 * allows players to create custom warp points to a location of their choice. 
 */
public class LocationManager extends FeatureBase {
    /**
     * Maximum number of characters on a single line when listing a player's warps using the "/warp
     * list" command. We should try to avoid wrapping here.
     */
    private static final int MAX_WARP_LIST_LINE_LENGTH = 50;
    
    /**
     * Database statement used to find locations in the database, based on their name and world.
     */
    private final DatabaseStatement mFindLocationStatement;
    
    /**
     * Database statement used to load saved location from the database using their location Id.
     */
    private final DatabaseStatement mLoadLocationStatement;
    
    /**
     * Database statement used to create a new location in the database.
     */
    private final DatabaseStatement mCreateLocationStatement;
    
    /**
     * Database statement used for finding the locations created by a certain player in a certain
     * world. Only the location names will be returned.
     */
    private final DatabaseStatement mListLocationsStatement;
    
    /**
     * Database statement used to remove a location from the database.
     */
    private final DatabaseStatement mRemoveLocationStatement;
    
    /**
     * Represents a location entry in the database, and allows the remove() method to be used on
     * it for conveniently removing it. The values will be initialized from the constructor, which
     * takes a DatabaseResultRow instance as its parameter to read information from.
     */
    class SavedLocation {
        public int location_id;
        public int user_id;
        public String name;
        public int password;
        public int world_hash;
        public int position_x;
        public int position_y;
        public int position_z;
        public double position_yaw;
        public double position_pitch;
        
        /**
         * Initializes the SavedLocation instance based on information read from the database, as
         * passed in |resultRow|, which is a single row from the "locations" table.
         * 
         * @param resultRow The database row containing the location information.
         */
        public SavedLocation(DatabaseResultRow resultRow) {
            location_id = resultRow.getInteger("location_id").intValue();
            user_id = resultRow.getInteger("user_id").intValue();
            name = resultRow.getString("name");
            password = resultRow.getInteger("password").intValue();
            world_hash = resultRow.getInteger("world_hash").intValue();
            position_x = resultRow.getInteger("position_x").intValue();
            position_y = resultRow.getInteger("position_y").intValue();
            position_z = resultRow.getInteger("position_z").intValue();
            position_yaw = resultRow.getDouble("position_yaw");
            position_pitch = resultRow.getDouble("position_pitch");
        }
        
        /**
         * Creates a new instance of Bukkit's Location class based on this SavedLocation data. The
         * active list of worlds will be retrieved from 
         * 
         * @return A Bukkit Location object representing this saved location.
         */
        public Location toBukkitLocation() {
            for (World world : getServer().getWorlds()) {
                if (getWorldHash(world) != world_hash)
                    continue;
                
                // TODO: We should probably do some check here to see if the world should still be
                //       accessible by players. Let's do that once we've got a WorldManager.
                
                return new Location(world, (double) position_x, (double) position_y,
                        (double) position_z, (float) position_yaw, (float) position_pitch);
            }
            
            return null;
        }
    }
    
    /**
     * Initializes the LocationManager class by preparing the queries which will be used for
     * interacting with the database.
     *
     * @param params Feature initialization parameters, required for initializing FeatureBase.
     */
    public LocationManager(FeatureInitParams params) {
        super(params);
        
        mFindLocationStatement = getDatabase().prepare(
                "SELECT " +
                    "locations.* " +
                "FROM " +
                    "locations " +
                "WHERE " +
                    "locations.name = ? AND " +
                    "locations.world_hash = ? AND " +
                    "locations.is_valid = 1"
        );
        
        mLoadLocationStatement = getDatabase().prepare(
                "SELECT " +
                    "locations.* " +
                "FROM " +
                    "locations " +
                "WHERE " +
                    "locations.location_id = ?"
        );

        mCreateLocationStatement = getDatabase().prepare(
                "INSERT INTO " +
                    "locations (user_id, name, password, world_hash, position_x, position_y, position_z, position_yaw, position_pitch, is_valid) " +
                "VALUES " +
                    "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        );
        
        mListLocationsStatement = getDatabase().prepare(
                "SELECT " +
                    "locations.name " +
                "FROM " +
                    "locations " +
                "WHERE " +
                    "locations.user_id = ? AND " +
                    "locations.world_hash = ? AND " +
                    "locations.is_valid = 1 " +
                "ORDER BY " +
                    "location_id DESC"
        );
        
        mRemoveLocationStatement = getDatabase().prepare(
                "UPDATE " +
                    "locations " +
                "SET " +
                    "locations.is_valid = 0 " +
                "WHERE " +
                    "locations.location_id = ? AND " +
                    "locations.is_valid = 1"
        );
    }
    
    /**
     * Asynchronously finds a location based on the |locationName| in |world|. A promise will be
     * returned, which will be resolved when the location is available. If the location could not be
     * found, or there are database issues, then the promise will be rejected.
     * 
     * @param locationName  Name of the location to search for.
     * @param world         The world in which this location exists.
     * @return              A Promise, which will be resolved with the SavedLocation once available.
     */
    private Promise<SavedLocation> findLocation(String locationName, World world) {
        final Promise<SavedLocation> promise = new Promise<SavedLocation>();
        
        mFindLocationStatement.setString(1, locationName);
        mFindLocationStatement.setInteger(2, getWorldHash(world));
        mFindLocationStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                if (result.rows.size() == 0) {
                    promise.reject("The location does not exist in the database.");
                    return;
                }
                promise.resolve(new SavedLocation(result.rows.get(0)));
            }
            public void onRejected(PromiseError error) {
                getLogger().severe("Could not find a location in the database (table: locations): " + error.reason());
                promise.reject("The location could not be read from the database.");
            }
        });
        
        return promise;
    }
    
    /**
     * Asynchronously loads a location based on the |locationId|. This method does not care about
     * whether the promise is still valid or not. A promise will be returned, which will be resolved
     * when the location has been loaded, or rejected if it could not be loaded.
     * 
     * @param locationId    Id of the location which should be loaded.
     * @return              A Promise, which will be resolved with the SavedLocation once available.
     */
    private Promise<SavedLocation> findLocationById(int locationId) {
        final Promise<SavedLocation> promise = new Promise<SavedLocation>();
        
        mLoadLocationStatement.setInteger(1, locationId);
        mLoadLocationStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                if (result.rows.size() == 0)
                    promise.reject("The location does not exist in the database.");
                else
                    promise.resolve(new SavedLocation(result.rows.get(0)));
            }
            public void onRejected(PromiseError error) {
                getLogger().severe("Could not load a location from the database (table: locations): " + error.reason());
                promise.reject("The location could not be read from the database.");
            }
        });
        
        return promise;
    }
    
    /**
     * Asynchronously creates a new location in the database. The new location will be owned by
     * |player|, which is also where the initial position will be read from. It will be accessible
     * using |locationName| when searching for locations in the player's current world.
     * 
     * A Promise will be returned, which will be resolved with the location Id when it has been
     * created in the database. If it could not be created for any reason, the promise will be
     * rejected with an error message containing more information.
     * 
     * @param player        The player who is creating the new location.
     * @param location      Exact location which should be saved in the database.
     * @param locationName  Name of the location, as it should be available.
     * @param password      Optional password with which the location should be protected.
     * @return              A promise, which will be resolved with the location Id.
     */
    private Promise<Integer> createLocation(final Player player, Location location, String locationName, String password) {
        final Promise<Integer> promise = new Promise<Integer>();
        final Account account = getAccountForPlayer(player);
        
        boolean disabledByDefault = false;
        
        // A little bit of magic which allows us to more conveniently create home warp Ids. If the
        // locationName hasn't been supplied, then this will be the home warp of a player.
        if (locationName.isEmpty()) {
            disabledByDefault = true;
            locationName = "[home]";
        }
        
        // We need the player's account to get their user Id, required for creating a warp point.
        if (account == null || account.getUserId() == 0) {
            promise.reject("Your account has not loaded properly, please reconnect or contact an administrator.");
            return promise;
        }
        
        // Hash the password using SimpleHash if it's more than an empty string.
        int passwordHash = password.isEmpty() ? 0 : SimpleHash.createHash(password);

        mCreateLocationStatement.setInteger(1, account.getUserId());
        mCreateLocationStatement.setString(2, locationName);
        mCreateLocationStatement.setInteger(3, passwordHash);
        mCreateLocationStatement.setInteger(4, getWorldHash(player.getWorld()));
        mCreateLocationStatement.setInteger(5, (long) location.getX());
        mCreateLocationStatement.setInteger(6, (long) (location.getY() + 0.5));
        mCreateLocationStatement.setInteger(7, (long) location.getZ());
        mCreateLocationStatement.setDouble(8, location.getYaw());
        mCreateLocationStatement.setDouble(9, location.getPitch());
        mCreateLocationStatement.setInteger(10, disabledByDefault ? 0 : 1);
        mCreateLocationStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                if (result.insertId == 0)
                    promise.reject("The warp could not be created because of an unknown database error.");
                else
                    promise.resolve(result.insertId);
            }
            public void onRejected(PromiseError error) {
                promise.reject("The warp could not be created because of an unknown database error.");
                getLogger().severe(error.reason());
            }
        });
        
        return promise;
    }
    
    /**
     * Asynchronously compiles a list of all locations which |player| has created in their current
     * world. When available, the returned Promise will be resolved with an ArrayList. The Promise
     * will be rejected when no locations could be found for the current world.
     * 
     * @param player    The player to find all locations for.
     * @param world     The world to get the player's saved locations for.
     * @return          A Promise, which will be resolved with a list of their locations.
     */
    private Promise<List<String>> listLocations(final Player player, World world) {
        final Promise<List<String>> promise = new Promise<List<String>>();
        
        mListLocationsStatement.setInteger(1, getUserId(player));
        mListLocationsStatement.setInteger(2, getWorldHash(world));
        mListLocationsStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                if (result.rows.size() == 0) {
                    promise.reject("You haven't saved any locations in the current world!");
                    return;
                }
                
                List<String> locations = new ArrayList<String>(result.rows.size());
                for (DatabaseResultRow resultRow : result.rows)
                    locations.add(resultRow.getString("name"));
                
                promise.resolve(locations);
            }
            public void onRejected(PromiseError error) {
                getLogger().severe("Unable to list locations from the database: " + error.reason());
                promise.reject("Unable to read your locations from the database, please talk to an administrator!");
            }
        });
        
        return promise;
    }
    
    /**
     * Asynchronously removes the saved location |location| from the world it's created in. Because
     * records in the database may depend on this record to exist, rather than actually removing
     * the saved location we'll flip the is_valid flag to false.
     * 
     * @param location  The location which should no longer be usable.
     * @return          A Promise, which will be resolved once it's been removed.
     */
    private Promise<Void> removeLocation(SavedLocation location) {
        final Promise<Void> promise = new Promise<Void>();
        
        mRemoveLocationStatement.setInteger(1, location.location_id);
        mRemoveLocationStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                if (result.affectedRows == 1)
                    promise.resolve(null);
                else
                    promise.reject("The query was successful, but no rows were affected.");
            }
            public void onRejected(PromiseError error) {
                getLogger().severe("Unable to remove a saved location from the database: " + error.reason());
                promise.reject("The database query failed.");
            }
        });
        
        return promise;
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
            createLocation(player, location, "", "").then(new PromiseResultHandler<Integer>() {
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
        
        findLocationById(locationId).then(new PromiseResultHandler<SavedLocation>() {
            public void onFulfilled(SavedLocation location) {
                Location destination = location.toBukkitLocation();
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
            
            findLocation(locationName, world).then(new PromiseResultHandler<SavedLocation>() {
                public void onFulfilled(SavedLocation result) {
                    displayCommandError(player, "A warp named \"" + locationName + "\" already exists in this world.");
                }
                public void onRejected(PromiseError error) {
                    // It may seem odd to do the work in onRejected(), but in case of findLocation
                    // it means that the location was *not* found, which is what we want when the
                    // player tries to create a new location in a certain world.
                    createLocation(player, location, locationName, password).then(new PromiseResultHandler<Integer>() {
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
            listLocations(player, world).then(new PromiseResultHandler<List<String>>() {
                public void onFulfilled(List<String> locations) {
                    displayCommandSuccess(player, "We found " + locations.size() + " saved locations in the database!");

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
            
            findLocation(locationName, world).then(new PromiseResultHandler<SavedLocation>() {
                public void onFulfilled(final SavedLocation location) {
                    // Players normally are only allowed to remove their own warps, but members of
                    // Mineground's staff will be allowed to remove any warp from the world.
                    if (location.user_id != getUserId(player) && !player.hasPermission("warp.remove_all")) {
                        displayCommandError(player, "You can only remove your own saved locations!");
                        return;
                    }
                    
                    removeLocation(location).then(new PromiseResultHandler<Void>() {
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

        findLocation(destination, world).then(new PromiseResultHandler<SavedLocation>() {
            public void onFulfilled(SavedLocation location) {
                // If the location has been protected by a password, this needs to be verified. Only
                // players with the warp.teleport_no_password permission are able to override this.
                if (location.password != 0 && !player.hasPermission("warp.teleport_no_password")) {
                    if (SimpleHash.createHash(password) != location.password) {
                        displayCommandError(player, "You need to enter the right password for the location \"" + destination + "\".");
                        return;
                    }
                }
                
                // Records that the player teleported to |location|.
                PlayerLog.record(RecordType.WARP_TELEPORTED, getUserId(player), location.location_id);
                
                displayCommandSuccess(player, "You have been teleported to " + destination + "!");
                player.teleport(location.toBukkitLocation());
            }
            public void onRejected(PromiseError error) {
                displayCommandError(player, "The location \"" + destination + "\" does not exist in this world.");
            }
        });
    }
}
