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
import com.mineground.base.WorldUtils;

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
     * Map between a world's hash value and under what conditions PVP should be allowed in there.
     */
    private final Map<Integer, PvpSetting> mWorldPvpSetting;

    public WorldManager(FeatureInitParams params) {
        super(params);
        
        // Initialize the commands component of the World Manager.
        mCommands = new WorldCommands(this, params);
        
        // TODO: Implement enforcing the PvpSetting directive if it's PvpDefault.
        // TODO: Implement loading PvpSettings and other world settings from the database.
        mWorldPvpSetting = new HashMap<Integer, PvpSetting>();
    }
    
    /**
     * Returns whether PVP is allowed for |world|. If the setting is not yet available in the
     * |mWorldPvpSetting| map, it will be assumed based on the world's own settings.
     * 
     * @param world The world to get to know about whether PVP is allowed.
     * @return      Whether PVP is allowed in the given world.
     */
    public PvpSetting getPlayerVersusPlayer(World world) {
        PvpSetting value = mWorldPvpSetting.get(WorldUtils.getWorldHash(world));
        if (value != null)
            return value;
        
        return world.getPVP() ? PvpSetting.PVP_ALLOWED : PvpSetting.PVP_DISALLOWED;
    }
    
    /**
     * Sets whether PVP should be allowed for |world|. The value will be stored in the database and
     * will thus persist between Mineground plugin reloads.
     * 
     * @param world     The world to change the PVP setting for.
     * @param setting   Whether PVP should be allowed, disallowed or by choice.
     */
    public void setPlayerVersusPlayer(World world, PvpSetting setting) {
        // TODO: Update the PVP value of this world in the database.
        
        mWorldPvpSetting.put(WorldUtils.getWorldHash(world), setting);
        if (setting == PvpSetting.PVP_DISALLOWED)
            world.setPVP(false);
        else
            world.setPVP(true);
    }
}
