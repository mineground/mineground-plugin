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

/**
 * Features have the ability to create components, which have to live alongside their own instance.
 * These components get access to the same parts of the Mineground plugin, but also have an extra
 * getFeature() method to retrieve the instance of the feature which owns them.
 */
public class FeatureComponent<FeatureClass> extends FeatureBase {
    /**
     * The feature which owns this component, stored in its own object type.
     */
    private final FeatureClass mFeature;
    
    /**
     * Initializes both the FeatureBase class, as well as the |mFeature| member of the component.
     */
    public FeatureComponent(FeatureClass feature, FeatureInitParams params) {
        super(params);
        
        mFeature = feature;
    }
    
    /**
     * Returns the feature which owns this component, as its own type.
     * 
     * @return The feature which owns this component.
     */
    protected FeatureClass getFeature() {
        return mFeature;
    }
}
