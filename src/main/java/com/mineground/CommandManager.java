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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mineground.base.CommandHandler;

/**
 * Commands are a critical part of creating an interactive server, as it allows more advanced
 * functionality such as warps and personal messaging to be implemented effectively. This class
 * routes commands to their designated handler, but keep in mind that the commands themselves will
 * still need to be defined in the plugin.yml file as well.
 */
public class CommandManager {
    /**
     * Private inner class representing the fact that |method| on |instance| handles a command. Weak
     * references are kept of command handler instances, because we don't want to keep features
     * alive (as we allow them to be enabled and disabled during runtime).
     */
    private class CommandHandlerRef {
        private WeakReference<Object> instance;
        private Method method;
        private boolean console;
        
        private CommandHandlerRef(Object instance_, Method method_, boolean console_) {
            instance = new WeakReference<Object>(instance_);
            method = method_;
            console = console_;
        }
    }
    
    /**
     * Map between a command name and the handler which will be controlling it.
     */
    private final Map<String, CommandHandlerRef> mCommandMap;
    
    /**
     * Logger used for outputting warnings and errors which occurred whilst executing a command.
     */
    private final Logger mLogger;

    public CommandManager(JavaPlugin plugin) {
        mCommandMap = new HashMap<String, CommandHandlerRef>();
        mLogger = Logger.getLogger(CommandManager.class.getCanonicalName());
    }

    /**
     * Registers all commands we can find in |instance|. Commands are identified by their mandatory
     * CommandHandler annotation. Permissions for the commands will be dealt with by Bukkit.
     * 
     * @param instance The object to scan for command handlers.
     */
    public void registerCommands(Object instance) {
        Method[] reflectionMethods = instance.getClass().getMethods();
        for (Method method : reflectionMethods) {
            final Annotation commandAnnotation = method.getAnnotation(CommandHandler.class);
            if (commandAnnotation == null)
                continue;
            
            CommandHandler command = (CommandHandler) commandAnnotation;
            mCommandMap.put(command.value(), new CommandHandlerRef(instance, method, command.console()));
        }
    }

    /**
     * Invoked when either the player or an operator through the console, identified by |sender|,
     * executes |command|, with |arguments| as the entered arguments.
     * 
     * @param sender    Origin of the command, can be a Player or a console object.
     * @param command   The command which they executed.
     * @param arguments Array of arguments passed to the command. 
     * @return          Whether the command was routed successfully.
     */
    public boolean onCommand(CommandSender sender, Command command, String[] arguments) {
        final CommandHandlerRef observer = mCommandMap.get(command.getName());
        if (observer == null)
            return false;
        
        final Object instance = observer.instance.get();
        
        // Check whether the instance on which this command was defined is still alive. If it has
        // been garbage collected, then we should unregister this command from Bukkit.
        if (instance == null) {
            mCommandMap.remove(command.getName());
            return false;
        }
        
        // Check whether the command may be executed on the console, if it executed by a non-Player.
        if (!(sender instanceof Player) && observer.console == false) {
            sender.sendMessage("The command /" + command.getName() + " is not available from the console.");
            return true;
        }
        
        
        // Execute the command by invoking the method, and returning the return value (which should
        // be a boolean) to the caller of onCommand.
        try {
            observer.method.invoke(instance, sender, arguments);
            return true;

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            mLogger.severe("An exception occurred while attempting to execute the command /" + command.getName() + ":");
            e.printStackTrace();
        }

        return false;
    }
}
