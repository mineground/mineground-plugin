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
import org.bukkit.entity.Player;

import com.mineground.account.Account;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Promise;
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
import com.mineground.base.SimpleHash;
import com.mineground.base.WorldUtils;
import com.mineground.database.DatabaseResult;
import com.mineground.database.DatabaseResultRow;
import com.mineground.database.DatabaseStatement;

/**
 * The Warp Manager owns the /warp command, which is one of Mineground's most popular commands as it
 * allows players to create custom warp points to a location of their choice. 
 */
public class LocationManager extends FeatureBase {
    /**
     * Instance of the LocationCommands class, which implements all the commands available to both
     * players and staff to deal with location-related matters. It's a feature component owned by
     * this feature, so its lifetime will be coupled.
     */
    @SuppressWarnings("unused")
    private final LocationCommands mCommands;
    
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
     * Initializes the LocationManager class by preparing the queries which will be used for
     * interacting with the database.
     *
     * @param params Feature initialization parameters, required for initializing FeatureBase.
     */
    public LocationManager(FeatureInitParams params) {
        super(params);
        
        // Initialize the commands component of the Location Manager.
        mCommands = new LocationCommands(this, params);
        
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
    public Promise<LocationRecord> findLocation(String locationName, World world) {
        final Promise<LocationRecord> promise = new Promise<LocationRecord>();
        
        mFindLocationStatement.setString(1, locationName);
        mFindLocationStatement.setInteger(2, WorldUtils.getWorldHash(world));
        mFindLocationStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                if (result.rows.size() == 0) {
                    promise.reject("The location does not exist in the database.");
                    return;
                }
                promise.resolve(new LocationRecord(result.rows.get(0)));
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
    public Promise<LocationRecord> findLocationById(int locationId) {
        final Promise<LocationRecord> promise = new Promise<LocationRecord>();
        
        mLoadLocationStatement.setInteger(1, locationId);
        mLoadLocationStatement.execute().then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                if (result.rows.size() == 0)
                    promise.reject("The location does not exist in the database.");
                else
                    promise.resolve(new LocationRecord(result.rows.get(0)));
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
    public Promise<Integer> createLocation(final Player player, Location location, String locationName, String password) {
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
        mCreateLocationStatement.setInteger(4, WorldUtils.getWorldHash(player.getWorld()));
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
    public Promise<List<String>> listLocations(final Player player, World world) {
        final Promise<List<String>> promise = new Promise<List<String>>();
        
        mListLocationsStatement.setInteger(1, getUserId(player));
        mListLocationsStatement.setInteger(2, WorldUtils.getWorldHash(world));
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
    public Promise<Void> removeLocation(LocationRecord location) {
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
}
