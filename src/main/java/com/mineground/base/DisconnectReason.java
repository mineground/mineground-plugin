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

/**
 * Enumaration containing the reasons as to why a player left Mineground. Different from the Bukkit
 * API, Mineground supports a single event capturing all reasons why a player has disconnected.
 */
public enum DisconnectReason {
    /**
     * The player has left Mineground by purposefully disconnecting from the server.
     */
    QUIT,
    
    /**
     * The player has left Mineground because the plugin is being shut down.
     */
    SHUTDOWN
}
