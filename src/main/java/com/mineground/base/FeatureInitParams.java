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

import org.bukkit.Server;
import org.bukkit.configuration.Configuration;

import com.mineground.CommandManager;
import com.mineground.EventDispatcher;
import com.mineground.FeatureManager;

// Initialization parameters which features receive and must pass on to their FeatureBase parent
// class, allowing it to initialize itself to make all features available.
public class FeatureInitParams {
    public FeatureManager featureManager;
    public CommandManager commandManager;
    public EventDispatcher eventDispatcher;
    public Configuration configuration;
    public Server server;
}
