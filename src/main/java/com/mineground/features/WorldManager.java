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

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;

import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;

/**
 * Minecraft supports an arbitrary amount of worlds existing simultaneously, each with their own
 * settings and rules. Management members have the ability to create, remove and change various
 * critical settings of these rules, while each player will have the ability to teleport back and
 * forth between certain ones of them.
 */
public class WorldManager extends FeatureBase {
    /**
     * Instance of the WorldCommands class, which implements all the commands available to both
     * players and staff to deal with world-related matters.
     */
    @SuppressWarnings("unused")
    private final WorldCommands mCommands;
    
    /**
     * The default world on Mineground. This is where all new players will spawn in. It can be
     * reached by using the /survival command as well.
     */
    private World mDefaultWorld;

    /**
     * The creative world on Mineground. This is where players will be teleported when they use
     * the /creative command. It can be updated by administrators using /world.
     */
    private World mCreativeWorld;
    
    /**
     * The classic world on Mineground. This is where players will be teleported when they use the
     * /classic command. It can be updated by administrators using /world.
     */
    private World mClassicWorld;
    
    /**
     * A map between a world instance and the settings applying to that world.
     */
    private Map<World, WorldSettings> mWorldSettings;
    
    public WorldManager(FeatureInitParams params) {
        super(params);
        
        // Initialize the commands component of the World Manager.
        mCommands = new WorldCommands(this, params);
        
        mDefaultWorld = getServer().getWorld(getSettings().getString("worlds.default", ""));
        if (mDefaultWorld == null)
            mDefaultWorld = getServer().getWorlds().get(0);
        
        mCreativeWorld = getServer().getWorld(getSettings().getString("worlds.creative", ""));
        mClassicWorld = getServer().getWorld(getSettings().getString("worlds.classic", ""));
        
        mWorldSettings = new HashMap<World, WorldSettings>();
    }
    
    /**
     * Returns the WorldSettings instance for <code>world</code>.
     * 
     * @param world The world to get the settings for.
     * @return      The WorldSettings instance for <code>world</code>.
     */
    public WorldSettings getWorldSettings(World world) {
        WorldSettings worldSettings = mWorldSettings.get(world);
        if (worldSettings != null)
            return worldSettings;
        
        worldSettings = new WorldSettings(world, getSettings());
        mWorldSettings.put(world, worldSettings);
        return worldSettings;
    }
    
    /**
     * Must be invoked when a world is being removed. This will make sure that we release all
     * references to <code>world</code>, avoiding memory leaks.
     * 
     * @param world The world which is being removed.
     */
    public void onRemoveWorld(World world) {
        mWorldSettings.remove(world);
    }
    
    /**
     * Returns the default world in Mineground. The spawn position in this world is where all new
     * players will be spawned.
     * 
     * @return  Mineground's default world.
     */
    public World getDefaultWorld() {
        return mDefaultWorld;
    }
    
    /**
     * Updates the default world to another one. This will persist between server sessions. The
     * <code>defaultWorld</code> argument must not be null.
     * 
     * @param defaultWorld  The world which should become Mineground's default world.
     */
    public void setDefaultWorld(World defaultWorld) {
        getSettings().set("worlds.default", defaultWorld.getName());
        getSettings().save();

        mDefaultWorld = defaultWorld;
    }
    
    /**
     * Returns the current creative world on Mineground. This is the world that players will be
     * teleported to when using /creative.
     * 
     * @return  The creative world.
     */
    public World getCreativeWorld() {
        return mCreativeWorld;
    }
    
    /**
     * Updates the creative world to point to <code>creativeWorld</code>.
     * 
     * @param creativeWorld The world which will now be known as the creative world.
     */
    public void setCreativeWorld(World creativeWorld) {
        getSettings().set("worlds.creative", creativeWorld == null ? "" : creativeWorld.getName());
        getSettings().save();

        mCreativeWorld = creativeWorld;
    }
    
    /**
     * Returns the current classic world on Mineground. This will usually be the previous map in
     * read-only mode, but that can be changed by Management members.
     * 
     * @return  The classic world.
     */
    public World getClassicWorld() {
        return mClassicWorld;
    }
    
    /**
     * Updates the classic world to point to <code>classicWorld</code>.
     * 
     * @param classicWorld  The world which will now be known as the classic world.
     */
    public void setClassicWorld(World classicWorld) {
        getSettings().set("worlds.classic", classicWorld == null ? "" : classicWorld.getName());
        getSettings().save();

        mClassicWorld = classicWorld;
    }
}
