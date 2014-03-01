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

import org.bukkit.plugin.java.JavaPlugin;


// The Mineground class is the plugin which exposes our plugin to Bukkit. It has access to APIs for
// most of Bukkit's internals, and decides the lifetime of the rest of the mode.
public class Mineground extends JavaPlugin {
    // The core event listener and dispatcher in use for Mineground. Consumers of any kind of event
    // will be attached as observers to the event dispatcher.
    private EventDispatcher mEventDispatcher;
    
    @Override
    public void onEnable() {
        mEventDispatcher = new EventDispatcher();
        
        // TODO: Add further Mineground initialization.
        
        mEventDispatcher.onMinegroundLoaded();
    }
    
    @Override
    public void onDisable() {
        mEventDispatcher.onMinegroundUnloaded();
    }
}
