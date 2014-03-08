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

import java.io.File;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mineground.account.AccountManager;
import com.mineground.base.FeatureInitParams;
import com.mineground.database.Database;

// The Mineground class is the plugin which exposes our plugin to Bukkit. It has access to APIs for
// most of Bukkit's internals, and decides the lifetime of the rest of the mode.
public class Mineground extends JavaPlugin {
    // The core event listener and dispatcher in use for Mineground. Consumers of any kind of event
    // will be attached as observers to the event dispatcher.
    private EventDispatcher mEventDispatcher;
    
    // Class used for listening to incoming events from Bukkit, which allows Mineground to respond
    // to actions and events generated by the players on the server.
    private EventListener mEventListener;
    
    // Class used for routing commands executed by the player to the feature which implements them.
    private CommandManager mCommandManager;
    
    // Class used for managing all features implemented in the Mineground plugin.
    private FeatureManager mFeatureManager;
    
    // The account manager curates the accounts of all players on Mineground. Each player implicitly
    // receives an account, which they can activate by registering on the website.
    private AccountManager mAccountManager;
    
    // 
    private ChatManager mChatManager;
    
    // Mineground uses a separate YML file in its data directory for configuration of this plugin.
    // The instance is writable, and will be made available to every feature. The file is stored
    // outside of the jar to avoid needing to rebuild it when a setting changes.
    private FileConfiguration mConfiguration;
    private File mConfigurationFile;
    
    // Mineground stores pretty much all information (beyond the world data) in a database, ensuring
    // that it persists between sessions and can be accessed from outside this plugin as well. The
    // Database implementation is the main API for that.
    private Database mDatabase;
    
    @Override
    public void onEnable() {
        // Initializes the Mineground-specific configuration (which should reside in the plugin's
        // data folder). If the data folder does not exist yet, it will be created.
        final File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdir())
            getLogger().severe("Could not create the data folder for the Mineground plugin.");
        
        mConfigurationFile = new File(dataFolder, "mineground.yml");
        mConfiguration = YamlConfiguration.loadConfiguration(mConfigurationFile);
        
        // Initialize the Database API and ensure that it can connect to actual database powering
        // it. Without database access, Mineground will be significantly limited in functionality.
        mDatabase = new Database(mConfiguration, this);
        mDatabase.connect();

        mAccountManager = new AccountManager(mDatabase);
        mChatManager = new ChatManager();
        
        mEventDispatcher = new EventDispatcher();
        mEventListener = new EventListener(mEventDispatcher, mAccountManager, mChatManager);
        
        mCommandManager = new CommandManager(this);

        // Register |mEventListener| with Bukkit's Plugin Manager, so it will receive events.
        getServer().getPluginManager().registerEvents(mEventListener, this);

        // The Feature Manager will initialize all individual features available on Mineground,
        // which includes giving them the ability to listen for the |onMinegroundLoaded| event.
        // Features require access to a large amount of internals, passed on in FeatureInitParams.
        FeatureInitParams featureInitParams = new FeatureInitParams();
        featureInitParams.commandManager = mCommandManager;
        featureInitParams.eventDispatcher = mEventDispatcher;
        featureInitParams.configuration = mConfiguration;
        featureInitParams.database = mDatabase;
        featureInitParams.accountManager = mAccountManager;
        featureInitParams.server = getServer();
        
        // Instantiate the Feature Manager itself, with the parameters as we previously compiled.
        mFeatureManager = new FeatureManager(featureInitParams);
        mFeatureManager.initializeFeatures();
        
        mEventDispatcher.onMinegroundLoaded();
        
        // If there are already players around on the server, we need to inform the account manager
        // and all features about them being here. Treat them as if they're just joining.
        for (Player player : getServer().getOnlinePlayers())
            mAccountManager.loadAccount(player, mEventDispatcher);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        return mCommandManager.onCommand(sender, command, arguments);
    }
    
    @Override
    public void onDisable() {
        // If there still are players on the server, inform the account manager and all the features
        // as if they're all leaving the server at the same time right now.
        for (Player player : getServer().getOnlinePlayers()) {
            mEventDispatcher.onPlayerQuit(player);
            mAccountManager.unloadAccount(player);
        }
        
        // Fire the onMinegroundUnloaded event, telling all features that they must clean up.
        mEventDispatcher.onMinegroundUnloaded();

        // And NULL all the main instances in Mineground, which should clean up all remaining state,
        // close open connections, so that we can leave with a clear conscience.
        mFeatureManager = null;
        mCommandManager = null;

        mEventListener = null;
        mEventDispatcher = null;
        
        mChatManager = null;
        mAccountManager = null;
        
        mDatabase.disconnect();
        mDatabase = null;
        
        mConfiguration = null;
        mConfigurationFile = null;
    }
}
