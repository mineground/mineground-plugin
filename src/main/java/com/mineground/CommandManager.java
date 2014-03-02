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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

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
    private JavaPlugin mPlugin;

    // Initializes our local instances of the Bukkit command map, as well as Bukkit's PluginCommand
    // constructor, and verifies that we can use them.
    public CommandManager(JavaPlugin plugin) {
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
            
            // TODO: Register the command internally in the CommandManager.
        }
    }
    
    // Invoked when either the player or an operator through the console, identified by |sender|,
    // executes |command|, with |arguments| as the entered arguments.
    public boolean onCommand(CommandSender sender, Command command, String[] arguments) {
        // TODO: Dispatch the command to the method which registered it. If the instance on which
        //       the method lived no longer is around, we need to unregister the command from Bukkit.
        return false;
    }
}
