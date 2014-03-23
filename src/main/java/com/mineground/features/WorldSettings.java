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

import org.bukkit.World;

import com.mineground.base.Settings;
import com.mineground.base.WorldUtils;

/**
 * The WorldSettings class contains the settings which apply to a certain world. All instances of
 * this class are curated by the WorldManager.
 */
public class WorldSettings {
    /**
     * The world for which' settings this instance is responsible.
     */
    private final World mWorld;

    /**
     * Mineground's settings instance, in which we save all changes made to this world.
     */
    private final Settings mSettings;
    
    /**
     * Path to the settings for this unique world.
     */
    private final String mSettingsPath;
    
    /**
     * Possible values for setting whether player-versus-player fighting is allowed in the world.
     */
    public enum PvpSetting {
        /**
         * PVP is always allowed, regardless of the player's preference.
         */
        PVP_ALLOWED,
        
        /**
         * PVP is never allowed, regardless of the player's preference.
         */
        PVP_DISALLOWED,
        
        /**
         * PVP is allowed or disallowed based on the player's preference.
         */
        PVP_DEFAULT
    }
    
    /**
     * Setting indicating whether PVP fighting is allowed in this world.
     */
    private PvpSetting mPvpSetting = PvpSetting.PVP_DEFAULT;
    
    public WorldSettings(World world, Settings settings) {
        mWorld = world;
        mSettings = settings;
        
        mSettingsPath = "worlds.settings." + WorldUtils.getWorldHash(world) + ".";
        
        // Reads the |pvp| setting from the stored data.
        final String pvpSetting = get("pvp", "default");
        if (pvpSetting.equals("allowed"))
            mPvpSetting = PvpSetting.PVP_ALLOWED;
        else if (pvpSetting.equals("disallowed"))
            mPvpSetting = PvpSetting.PVP_DISALLOWED;
    }
    
    /**
     * Returns whether PVP is allowed in this world, and if so, under which circumstances.
     * 
     * @return Whether PVP should be allowed in this world.
     */
    public PvpSetting getPvp() {
        return mPvpSetting;
    }
    
    /**
     * Sets whether PVP should be allowed in this world. Changing this value will immediately write
     * it to the setting file.
     * 
     * @param value Whether PVP should be allowed in this world.
     */
    public void setPvp(PvpSetting value) {
        mPvpSetting = value;
        
        if (value == PvpSetting.PVP_ALLOWED)
            set("pvp", "allowed");
        else if (value == PvpSetting.PVP_DISALLOWED)
            set("pvp", "disallowed");
        else
            set("pvp", "default");
    }
    
    /**
     * Returns the stored value of <code>key</code> when it is available in the settings, or falls
     * back to <code>defaultValue</code> if it has not yet been saved.
     * 
     * @param key           Key of the field to read from the settings.
     * @param defaultValue  Default value, in case the field is not available.
     * @return              Textual value of either the stored setting, or the default value.
     */
    private String get(String key, String defaultValue) {
        return mSettings.getString(mSettingsPath + key, defaultValue);
    }
    
    /**
     * Updates the value of <code>key</code> to be <code>value</code>. This will immediately write
     * the change to the settings file.
     * 
     * @param key   Key of the field which is being changed.
     * @param value New value of this field.
     */
    private void set(String key, String value) {
        mSettings.set(mSettingsPath + key, value);
        mSettings.save();
    }
}
