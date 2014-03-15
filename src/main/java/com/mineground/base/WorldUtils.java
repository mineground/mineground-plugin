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

package com.mineground.base;

import org.bukkit.World;

/**
 * Utility methods related to working with the created Minecraft worlds on the server. Any method in
 * this class should be used by two or more features or components before being generalized.
 */
public class WorldUtils {
    /**
     * Returns a signed 32-bit integer representing a hashed value of |world|'s unique Id. This Id
     * will be persistent between server restarts, and is not dependent on the world's name.
     * 
     * @param world The world to get a hash of.
     * @return      A signed 32-bit integer representing |world|.
     */
    public static int getWorldHash(World world) {
        return SimpleHash.createHash(world.getUID().toString());
    }
}
