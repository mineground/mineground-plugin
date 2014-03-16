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

import com.mineground.base.WorldUtils;
import com.mineground.database.DatabaseResultRow;

/**
 * Represents a location entry as they are stored in the database. Instances may only be created
 * from database rows, but will publicly expose all the right information and a few convenience
 * methods, for example to convert it to Bukkit's location object.
 */
public class LocationRecord {
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
    public LocationRecord(DatabaseResultRow resultRow) {
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
     * @param worlds    A list of Worlds currently available on Mineground.
     * @return          A Bukkit Location object representing this saved location.
     */
    public Location toBukkitLocation(List<World> worlds) {
        for (World world : worlds) {
            if (WorldUtils.getWorldHash(world) != world_hash)
                continue;
            
            // TODO: We should probably do some check here to see if the world should still be
            //       accessible by players. Let's do that once we've got a WorldManager.
            
            return new Location(world, (double) position_x, (double) position_y,
                    (double) position_z, (float) position_yaw, (float) position_pitch);
        }
        
        return null;
    }
}
