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
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

import com.mineground.CommandManager;
import com.mineground.EventDispatcher;
import com.mineground.FeatureManager;
import com.mineground.account.Account;
import com.mineground.account.AccountManager;
import com.mineground.database.Database;

/**
 * Parent class for all features implemented in the Mineground plugin. It allows features to access
 * various parts of the Mineground core, allowing it to work in an effective, yet isolated way.
 */
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
        params.eventDispatcher.registerListeners(this);

        // Registers all commands defined in this feature with the CommandManager.
        params.commandManager.registerCommands(this);
    }
    
    /**
     * Returns the Logger instance, to which this feature can output information intended to the
     * console and the log files.
     * 
     * @return Feature-specific Logger instance.
     */
    protected Logger getLogger() { return mLogger; }
    
    /**
     * Returns the Feature Manager, which this feature can query to try and get access to other
     * features implemented in Mineground.
     * 
     * @return Shared FeatureManager instance.
     */
    protected FeatureManager getFeatureManager() {
        return mInitParams.featureManager;
    }

    /**
     * Returns the Command Manager. Features don't need to have all their functionality in a single
     * class, and may want to separate out commands. When doing that, the new object needs to be
     * registered with the command manager, which will hold a (weak!) pointer to it.
     * 
     * @return Shared CommandManager instance.
     */
    protected CommandManager getCommandManager() {
        return mInitParams.commandManager;
    }
    
    /**
     * Returns the event dispatcher. Features don't need to have all their functionality in a single
     * class, and may want to separate out event handlers. When doing that, the new object needs to
     * be registered with the event dispatcher, which will hold a (weak!) pointer to it.
     * 
     * @return Shared EventDispatcher instance.
     */
    protected EventDispatcher getEventDispatcher() {
        return mInitParams.eventDispatcher;
    }
    
    /**
     * Returns the configuration interface to work with the mineground.yml file.
     * 
     * @return Shared Configuration instance.
     */
    protected Configuration getConfiguration() {
        return mInitParams.configuration;
    }
    
    /**
     * Returns the active database connection shared among Mineground. All queries set to be
     * executed on this connection will be added to a queue, and will be executed in order (FIFO).
     * 
     * @return Shared Database instance.
     */
    protected Database getDatabase() {
        return mInitParams.database;
    }
    
    /**
     * Returns the Account Manager instance, granting access to both more advanced operations based
     * on one or more accounts, and convenience methods for retrieving all users of a certain type.
     * 
     * @return Shared AccountManager instance.
     */
    protected AccountManager getAccountManager() {
        return mInitParams.accountManager;
    }
    
    /**
     * Returns the Account instance for |player|.
     * 
     * When called from event handlers, Account is guaranteed to be an object unless explicitly
     * stated in documentation for the event. Outside of event handlers, be sure to do a NULL check.
     * 
     * @param player    Player to retrieve the account for.
     * @return          Account instance specific to that player.
     */
    protected Account getAccountForPlayer(Player player) {
        return mInitParams.accountManager.getAccountForPlayer(player);
    }
    
    /**
     * Returns the Bukkit Server instance, which allows an individual feature to integrate much more
     * deeply with Bukkit. Please do keep in mind that any dependency on Bukkit will increase the
     * chance of the plugin breaking during an update.
     * 
     * @return Bukkit's Server instance.
     */
    protected Server getServer() {
        return mInitParams.server;
    }
}
