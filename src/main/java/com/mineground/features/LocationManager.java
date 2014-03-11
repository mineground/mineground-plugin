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

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Promise;
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
import com.mineground.base.SimplePasswordHash;
import com.mineground.database.DatabaseResult;
import com.mineground.database.DatabaseResultRow;
import com.mineground.database.DatabaseStatement;

/**
 * The Warp Manager owns the /warp command, which is one of Mineground's most popular commands as it
 * allows players to create custom warp points to a location of their choice. 
 */
public class LocationManager extends FeatureBase {
    /**
     * Database statement used to find locations in the database, based on their name and world.
     */
    private final DatabaseStatement mFindLocationStatement;
    
    /**
     * Database statement used to create a new location in the database.
     */
    private final DatabaseStatement mCreateLocationStatement;
    
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
        public long password;
        public String world;
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
            password = resultRow.getInteger("password");
            world = resultRow.getString("world");
            position_x = resultRow.getInteger("position_x").intValue();
            position_y = resultRow.getInteger("position_y").intValue();
            position_z = resultRow.getInteger("position_z").intValue();
            position_yaw = resultRow.getDouble("position_yaw");
            position_pitch = resultRow.getDouble("position_pitch");
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
                    "locations.world = ?"
        );

        mCreateLocationStatement = getDatabase().prepare("");
        mRemoveLocationStatement = getDatabase().prepare("");
        
    }
    
    /**
     * Asynchronously finds a location based on the |locationName| in |worldName|. A promise will
     * be returned, which will be resolved when the location is available. If the location could
     * not be found, or there are database issues, then the promise will be rejected.
     * 
     * @param locationName  Name of the location to search for.
     * @param worldName     The world in which this location exists.
     * @return              A Promise, which will be resolved with the Location once available.
     */
    private Promise<SavedLocation> findLocation(String locationName, String worldName) {
        final Promise<SavedLocation> promise = new Promise<SavedLocation>();
        
        mFindLocationStatement.setString(1, locationName);
        mFindLocationStatement.setString(2, worldName);
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
     * Asynchronously compiles a list of all locations which |player| has created in their current
     * world. When available, the returned Promise will be resolved with an ArrayList, even if there
     * are no locations. The Promise will only be rejected if there was a database problem.
     * 
     * @param player    The player to find all locations for.
     * @return          A Promise, which will be resolved with a list of their locations.
     */
    private Promise<List<SavedLocation>> listLocations(final Player player) {
        // TODO: Implement this method.
        return null;
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
     * @param locationName  Name of the location, as it should be available.
     * @param password      Optional password with which the location should be protected.
     * @return              A promise, which will be resolved with the location Id.
     */
    private Promise<Integer> createLocation(final Player player, String locationName, String password) {
        // TODO: Implement this method.
        return null;
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
     // TODO: Implement this command.
    }

    /**
     * Implements the /spawn command, which is a convenient way for players to teleport to the spawn
     * in the world they're currently in. Certain staff members may be granted a permission which
     * allows them to set the spawn position for a given world.
     * 
     * /spawn           Teleports the player to the spawn position of their current world.
     * /spawn set       Updates the spawn position to the player's current location.
     * 
     * @param sender    The Player or console who executed this command.
     * @param arguments The arguments which were passed on to this command.
     */
    @CommandHandler("spawn")
    public void onSpawnCommand(CommandSender sender, String[] arguments) {
     // TODO: Implement this command.
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
        
        // TODO: Implement /warp create.
        // TODO: Implement /warp list.
        // TODO: Implement /warp remove.
        
        // The player wants to teleport to a previously created warp in the current world. Find the
        // warp in the database, make sure that the password matches, and then teleport them.
        final String destination = arguments[0];
        final String password = (arguments.length >= 2) ? arguments[1] : "";
        
        if (!player.hasPermission("warp.teleport")) {
            displayCommandError(player, "You are not allowed to teleport to saved locations.");
            return;
        }

        findLocation(destination, world.getName()).then(new PromiseResultHandler<SavedLocation>() {
            public void onFulfilled(SavedLocation location) {
                // If the location has been protected by a password, this needs to be verified. Only
                // players with the warp.teleport_no_password permission are able to override this.
                if (location.password != 0 && !player.hasPermission("warp.teleport_no_password")) {
                    if (!SimplePasswordHash.validatePassword(password, location.password)) {
                        displayCommandError(player, "You need to enter the right password for the location \"" + destination + "\".");
                        return;
                    }
                }
                
                // TODO: Register this teleportation with the PlayerLog.
                
                displayCommandSuccess(player, "You have been teleported to " + destination + "!");
                player.teleport(new Location(world, (double) location.position_x, (double) location.position_y,
                        (double) location.position_z, (float) location.position_yaw, (float) location.position_pitch));
            }

            public void onRejected(PromiseError error) {
                displayCommandError(player, "The location \"" + destination + "\" is not available in this world.");
            }
        });
    }
}
