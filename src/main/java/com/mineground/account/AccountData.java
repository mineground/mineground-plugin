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

import java.util.Date;

import org.bukkit.entity.Player;

// The AccountData class holds information about the player's profile, as it is stored in the
// database. When adding or removing fields, please make sure they are listed in the same order as
// they are in the actual database scheme, making this class easier to work with.
public class AccountData {

    // ***** users *********************************************************************************
    
    // Id of the player, which roughly corresponds to the order in which users registered.
    public int user_id;
    
    // The player's username, which is not necessarily equal to their display name.
    public String username;

    // Mineground uses PBKDF2 to store player passwords, for which the player's hashed password and
    // salt used to hash that password are both stored in this string. The password can be validated
    // by using Taylor Hornby's PBKDF2 password hash implementation.
    public String password;
    
    // The level this player's account has. This value is not mutable from the plugin, because it
    // would allow permanent promotions, which we'd like to limit to IRC.
    public AccountLevel level;
    
    // The date on which the player first joined Mineground. This value is not mutable from the
    // plugin, because it doesn't make sense to change the registration date.
    public Date registered;
    
    // ***** users_settings ************************************************************************
    
    // The total number of seconds the player has spent online on Mineground. This does include
    // time during which the player was idle.
    public int online_time;
    
    // The number of living entities, including both players, monsters and other NPCs, the player
    // has killed during their playing on the server.
    public int kill_count;
    
    // The number of times the player has died, either by being killed or from other causes.
    public int death_count;
    
    // The number of reaction tests this player has won.
    public int stats_reaction;
    
    // The number of blocks this player has created, in all worlds and gamemodes.
    public int stats_blocks_created;
    
    // The number of blocks this player has destroyed, in all worlds and gamemodes.
    public int stats_blocks_destroyed;
    
    // The date at which the player last connected to the server.
    public Date last_seen;
    
    // ***** Constructor for default values ********************************************************
    
    public AccountData(Player player) {
        user_id = 0;
        username = player.getName();
        password = "";
        level = AccountLevel.Guest;
        registered = new Date();
        
        online_time = 0;
        kill_count = 0;
        death_count = 0;
        stats_reaction = 0;
        stats_blocks_created = 0;
        stats_blocks_destroyed = 0;
        last_seen = new Date();
    }
}
