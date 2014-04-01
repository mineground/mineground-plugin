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

package com.mineground;

/**
 * Command observers will be informed about various changes in the Command Manager.
 */
public interface CommandObserver {
    /**
     * Invoked when a command has been registered with the Command Manager.
     * 
     * @param name      Name of the newly registered command.
     * @param ingame    Whether the command is executable from in-game.
     * @param console   Whether the command is executable from the console.
     * @param remote    Whether the command is executable from remote sources.
     */
    public void onCommandRegistered(String name, boolean ingame, boolean console, boolean remote);
    
    /**
     * Invoked when a command has been removed from the Command Manager.
     * 
     * @param name  Name of the command which no longer is available.
     */
    public void onCommandRemoved(String name);
}
