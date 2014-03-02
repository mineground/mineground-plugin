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

public class DevelopmentLog extends FeatureBase {
    public DevelopmentLog(FeatureInitParams params) {
        super(params);
    }
    
    public void onMinegroundLoaded() {
        getLogger().info("onMinegroundLoaded()");
    }
    
    public void onMinegroundUnloaded() {
        getLogger().info("onMinegroundUnloaded()");
    }
    
    public void onPlayerJoined(Player player) {
        getLogger().info("Welcome on Mineground, " + player.getName());
    }
}
