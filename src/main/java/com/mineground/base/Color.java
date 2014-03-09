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

// The color class contains the colors available for formatting in Minecraft's chat. These colors
// may be used for both the in-game chat and console chat.
public final class Color {
    public static final String BLACK = "§0";
    public static final String DARK_BLUE = "§1";
    public static final String DARK_GREEN = "§2";
    public static final String DARK_AQUA = "§3";
    public static final String DARK_RED = "§4";
    public static final String DARK_PURPLE = "§5";
    public static final String GOLD = "§6";
    public static final String GRAY = "§7";
    public static final String DARK_GRAY = "§8";
    public static final String BLUE = "§9";
    public static final String GREEN = "§a";
    public static final String AQUA = "§b";
    public static final String RED = "§c";
    public static final String LIGHT_PURPLE = "§d";
    public static final String YELLOW = "§e";
    public static final String WHITE = "§f";
    
    // The following constants are the canonical colors for operations of the said type throughout
    // Mineground. This allows the colors of communication on the server to stay consistent.
    
    // Used for player status change messages, for example when a player joins or leaves.
    // ...
    
    // Used for important messages which we'd like the player to notice. Welcoming them to the
    // server could be part of this, but also more serious things such as demands from admins.
    public static final String IMPORTANT_MESSAGE = RED;
    
    // Used for serious errors from the Mineground plugin itself. The player won't immediately be
    // able to act upon those, but they will be of interest for administrators and developers.
    public static final String SCRIPT_ERROR = DARK_GRAY + TextDecoration.ITALIC;
    
    private Color() { /** Disallow instantiating this class. **/ }
}
