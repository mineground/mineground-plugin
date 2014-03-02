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

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Server;

import com.mineground.base.Feature;
import com.mineground.base.FeatureInitParams;
import com.mineground.features.DevelopmentLog;

// The feature manager is responsible for --and owns-- all features available on Mineground. While
// features have reasonably individual contexts, there is a limited communication channel available
// between features, which is being curated by the manager.
public class FeatureManager {
    // Map of the name and instances of all features loaded on Mineground.
    private Map<String, Feature> mFeatures;

    private Server mServer;
    private CommandManager mCommandManager;
    private EventDispatcher mEventDispatcher;
    
    public FeatureManager(Server server, CommandManager commandManager, EventDispatcher eventDispatcher) {
        mServer = server;
        mCommandManager = commandManager;
        mEventDispatcher = eventDispatcher;
        
        mFeatures = new HashMap<String, Feature>();
    }
    
    // Initializes all the individual features by calling their constructors with an instance of the
    // FeatureInitParams class, which contains settings required by the FeatureBase class to work.
    public void initializeFeatures() {
        FeatureInitParams params = new FeatureInitParams();
        params.featureManager = this;
        params.commandManager = mCommandManager;
        params.eventDispatcher = mEventDispatcher;
        params.server = mServer;
        
        // TODO: Should we implement a more formal dependency model between features? That would
        //       have quite significant impact for the initialization order of them.
        
        mFeatures.put("DevelopmentLog", new DevelopmentLog(params));
    }
    
    
}
