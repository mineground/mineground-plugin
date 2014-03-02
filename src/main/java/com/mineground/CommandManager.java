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

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.mineground.base.Feature;
import com.mineground.base.FeatureCommand;

// Commands are a critical part of creating an interactive server, as it allows more advanced
// functionality such as warps and personal messaging to be implemented effectively.
//
// This class does a lot reflection magic against Bukkit internals, but the benefits are sound. By
// registering commands directly in Bukkit's commandMap, the commands will be fully functional and
// usable from any plugin, for any player, using Bukkit's own permission system, whilst still being
// under our control. Furthermore, convenience features such as tab-complete will work as expected.
public class CommandManager {
    // The CommandMap implementation as used by Bukkit. Commands on Mineground are registered at
    // runtime, so we inject them immediately into the Bukkit server. Entries in the map are of
    // type |PluginCommand|, the constructor for which we also need to store.
    private Constructor<PluginCommand> mBukkitPluginCommandConstructor;
    private CommandMap mBukkitCommandMap;
    
    // Commands are owned by a plugin in Bukkit, so we need our instance of JavaPlugin.
    private final JavaPlugin mPlugin;
    
    // Private inner class representing the fact that |method| on |instance| handles a command.
    private class CommandObserver {
        private WeakReference<Object> instance;
        private Method method;
        private boolean console;
        
        private CommandObserver(Object instance_, Method method_, boolean console_) {
            instance = new WeakReference<Object>(instance_);
            method = method_;
            console = console_;
        }
    }
    
    // Map between a command name and the observer which should be handling it.
    private final Map<String, CommandObserver> mCommandMap;

    // Initializes our local instances of the Bukkit command map, as well as Bukkit's PluginCommand
    // constructor, and verifies that we can use them.
    public CommandManager(JavaPlugin plugin) {
        mCommandMap = new HashMap<String, CommandObserver>();
        mPlugin = plugin;
        
        final PluginManager pluginManager = plugin.getServer().getPluginManager();
        if (!(pluginManager instanceof SimplePluginManager)) {
            plugin.getLogger().severe("Bukkit's plugin manager changed from SimplePluginManager.");
            return;
        }

        // Attempt to initialize |mBukkitCommandMap| as a reference to the commandMap field on
        // Bukkit's SimplePluginManager implementation.
        try {
            Field commandMapField = pluginManager.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            
            mBukkitCommandMap = (CommandMap) commandMapField.get(pluginManager);
        } catch (NoSuchFieldException | SecurityException e) {
            plugin.getLogger().severe("Bukkit's SimplePluginManager no longer has the \"commandMap\" field.");
            return;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            plugin.getLogger().severe("Bukkit's SimplePluginManager.commandMap field is inaccessible.");
            return;
        }

        // Attempt to initialize |mBukkitPluginCommandConstructor| as a reference to the constructor
        // of Bukkit's internal PluginCommand implementation.
        try {
            mBukkitPluginCommandConstructor = 
                    PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            mBukkitPluginCommandConstructor.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            plugin.getLogger().severe("Bukkit's PluginCommand constructor disappeared, or its signature changed.");
            return;
        }
    }

    // Registers all commands we can find in |instance|. Commands are identified by their mandatory
    // Command annotation, which contains the name and permission required for the command.
    public void registerCommands(Object instance) {
        Method[] reflectionMethods = instance.getClass().getMethods();
        for (Method method : reflectionMethods) {
            final Annotation commandAnnotation = method.getAnnotation(FeatureCommand.class);
            if (commandAnnotation == null)
                continue;
            
            FeatureCommand command = (FeatureCommand) commandAnnotation;
            
            // Attempt to register the command with Bukkit. There are about five thousand reasons
            // why this can fail, so this is --once again-- wrapped in a large try/catch statement.
            try {
                PluginCommand pluginCommand = mBukkitPluginCommandConstructor.newInstance(command.value(), mPlugin);
                pluginCommand.setDescription(command.description());
                pluginCommand.setUsage(command.usage());
                pluginCommand.setAliases(Arrays.asList(command.aliases()));
                pluginCommand.setPermission(command.permission());
                pluginCommand.setPermissionMessage(command.permissionMessage());
                
                // Insert the command directly into Bukkit's command map.
                mBukkitCommandMap.register(command.value(), pluginCommand);
                
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                mPlugin.getLogger().severe("Unable to register command \"" + command.value() + "\" in Bukkit's commandMap.");
                return;
            }
            
            // Registers the command in our own command map, allowing us to dispatch it.
            mCommandMap.put(command.value(), new CommandObserver(instance, method, command.console()));
        }
    }
    
    // When a command has been registered by an object which no longer is alive, the feature has
    // likely been unloaded. Remove the command both from our internal mapping and from Bukkit's.
    private void unregisterCommand(Command command, CommandObserver observer) {
        mCommandMap.remove(command.getName());
        
        // TODO: Figure out a way to unregister commands from Bukkit.
    }
    
    // Invoked when either the player or an operator through the console, identified by |sender|,
    // executes |command|, with |arguments| as the entered arguments.
    public boolean onCommand(CommandSender sender, Command command, String[] arguments) {
        final CommandObserver observer = mCommandMap.get(command.getName());
        if (observer == null)
            return false;
        
        final Object instance = observer.instance.get();
        
        // Check whether the instance on which this command was defined is still alive. If it has
        // been garbage collected, then we should unregister this command from Bukkit.
        if (instance == null) {
            unregisterCommand(command, observer);
            return false;
        }
        
        // If the command was executed on the console, and it's been listed as a command which may
        // only be used from in-game, don't invoke the method either.
        if (!observer.console && !(sender instanceof Player)) {
            sender.sendMessage("This command may only be used from in-game.");
            return false;
        }
        
        // Execute the command by invoking the method, and returning the return value (which should
        // be a boolean) to the caller of onCommand.
        try {
            return (boolean) observer.method.invoke(instance, sender, command, arguments);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            mPlugin.getLogger().severe("An exception occurred while executing command /" + command.getName() + ":");
            e.printStackTrace();
        }

        return false;
    }
}
