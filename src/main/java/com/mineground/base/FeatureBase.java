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

import java.util.logging.Logger;

import org.bukkit.Server;

import com.mineground.FeatureManager;

// Parent class for all features implemented in the Mineground plugin. It allows features to access
// various parts of the Mineground core, allowing it to work in an effective, yet isolated way.
public class FeatureBase implements Feature {
    private final FeatureInitParams mInitParams;
    private final Logger mLogger;

    public FeatureBase(FeatureInitParams params) {
        // TODO: We could unpack FeatureInitParams and assign each entry to its own variable in the
        //       new instance, but I expect the impact to be rather low. Let's leave it for now,
        //       and revisit if performance becomes a problem.
        mInitParams = params;
        
        // Initialize a logger for this feature, which allows output to be skimmed in a powerful way
        // given that Java loggers are hierarchical (e.g. com.mineground.features.MyFeature).
        mLogger = Logger.getLogger(getClass().getCanonicalName());
        
        // Registers all event listeners defined in this feature with the EventDispatcher.
        params.eventDispatcher.registerFeature(this);

        // TODO: Register this feature with the Command Manager.
    }
    
    // Returns the Logger instance which is specific to this feature.
    protected Logger getLogger() { return mLogger; }
    
    protected FeatureManager getFeatureManager() { return mInitParams.featureManager; }
    protected Server getServer() { return mInitParams.server; }
}
