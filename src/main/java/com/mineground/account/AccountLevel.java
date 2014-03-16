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

package com.mineground.account;

import com.mineground.base.Color;

/**
 * This class contains enumerations for the account levels supported by Mineground. These must match
 * the "level" enumeration in the "users" table in the database.
 */
public enum AccountLevel {
    /**
     * Guests don't have registered their account on the website, and therefore have very limited
     * rights on the server. They can talk and use a limited amount of commands, but are not allowed
     * to build anywhere on the server.
     */
    Guest,
    
    /**
     * Builders have registered their account on the website, and have validated their e-mail
     * address. They have the ability to build outside protected areas on Mineground.
     */
    Builder,
    
    /**
     * Super Builders are experienced players who have demonstrated to be both responsible and
     * knowledgeable in their playing on Mineground. They have access to the /fly command.
     */
    SBuilder,
    
    /**
     * VIPs have donated to the Mineground community, and therefore have a special status. They
     * receive the same benefits as the SBuilder class.
     */
    VIP,
    
    /**
     * TODO: Describe this accurately.
     */
    Moderator,
    
    /**
     * TODO: Describe this accurately.
     */
    Administrator,
    
    /**
     * Management members have all possible rights on the server, and will be made server operators
     * with the Bukkit server as well, automagically giving them admin rights for all other plugins.
     */
    Management;
    
    /**
     * Returns the AccountLevel corresponding to the string |level|, per our database conventions.
     * If no 1:1 mapping between the text and a level can be found, Guest will be returned.
     * 
     * @param level Textual representation of a level.
     * @return      The enumeration value of that level.
     */
    public static AccountLevel fromString(String level) {
        switch (level) {
            case "Builder":
                return Builder;
            case "SBuilder":
                return SBuilder;
            case "VIP":
                return VIP;
            case "Moderator":
                return Moderator;
            case "Administrator":
                return Administrator;
            case "Management":
                return Management;
        }

        return Guest;
    }
    
    /**
     * Returns the String corresponding to the AccountLevel |level|.
     * 
     * @param level The level to return the textual representation for.
     * @return      Textual representation of |level|.
     */
    public static String toString(AccountLevel level) {
        switch (level) {
            case Builder:
                return "Builder";
            case SBuilder:
                return "SBuilder";
            case VIP:
                return "VIP";
            case Moderator:
                return "Moderator";
            case Administrator:
                return "Administrator";
            case Management:
                return "Management";
        }
        
        return "Guest";
    }
    
    /**
     * Returns a color, if any, which can be used to identify players of a certain level.
     * 
     * @param level The level to return the color for.
     * @return      The color to represent the level.
     */
    public static String colorFor(AccountLevel level) {
        switch (level) {
            case Management:
                return Color.RED;
            case Administrator:
                return Color.RED;
            case Moderator:
                return Color.BLUE;
            case VIP:
                return Color.DARK_GREEN;
            case SBuilder:
                return Color.BLUE;
            case Builder:
                return Color.BLUE;
        }
        
        return Color.WHITE;
    }
    
    /**
     * Returns whether |level| corresponds to the level of a staff member.
     *
     * @param level The level to check.
     * @return      Whether the level should be considered staff.
     */
    public static boolean isStaff(AccountLevel level) {
        return level == Moderator ||
                level == Administrator ||
                level == Management;
    }
}
