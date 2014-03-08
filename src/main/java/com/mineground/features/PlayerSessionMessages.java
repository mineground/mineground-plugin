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

import org.bukkit.entity.Player;

import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;

// When a player joins or leaves Mineground, we'd like to welcome them and inform the other players
// about their presence. Furthermore, when a player joins Mineground for the first time, we may want
// to be a little bit nicer and give them some money and inventory to start with.
public class PlayerSessionMessages extends FeatureBase {
    public PlayerSessionMessages(FeatureInitParams params) { super(params); }
    
    public void onPlayerJoined(Player player) {
        // TODO: Don't send a message if they already were on the server.
        player.sendMessage("Welcome back on Mineground, " + player.getName());
    }
    
    public void onPlayerQuit(Player player) {
        // TODO: Don't send a message if the Mineground plugin is being unloaded.
    }
}
