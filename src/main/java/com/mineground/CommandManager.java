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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mineground.base.CommandCompletionHandler;
import com.mineground.base.CommandHandler;
import com.mineground.remote.RemoteCommandSender;

/**
 * Commands are a critical part of creating an interactive server, as it allows more advanced
 * functionality such as warps and personal messaging to be implemented effectively. This class
 * routes commands to their designated handler, but keep in mind that the commands themselves will
 * still need to be defined in the plugin.yml file as well.
 */
public class CommandManager implements TabCompleter {
    /**
     * Private inner class representing the fact that |method| on |instance| handles a command. Weak
     * references are kept of command handler instances, because we don't want to keep features
     * alive (as we allow them to be enabled and disabled during runtime).
     */
    private class CommandHandlerRef {
        private WeakReference<Object> instance;
        private Method autocomplete;
        private Method method;
        private boolean console;
        private boolean remote;
        
        private CommandHandlerRef(Object instance_, Method method_, boolean console_, boolean remote_) {
            instance = new WeakReference<Object>(instance_);
            autocomplete = null;
            method = method_;
            console = console_;
            remote = remote_;
        }
        
        private CommandHandlerRef(Object instance_, Method autocomplete_) {
            instance = new WeakReference<Object>(instance_);
            autocomplete = autocomplete_;
            method = null;
            console = false;
            remote = false;
        }
    }
    
    /**
     * Map between a command name and the handler which will be handling it, as well as the handler
     * which is responsible for providing auto-complete suggestions for this command.
     */
    private final Map<String, CommandHandlerRef> mCommandMap;
    
    /**
     * Map between an alias name and the command it should map to.
     */
    private final Map<String, String> mCommandAliasMap;
    
    /**
     * List of command observers. Each observer will have a method invoked whenever a certain event
     * happens in the command manager, most likely adding or removing a command.
     */
    private final List<CommandObserver> mCommandObservers;
    
    /**
     * The plugin which owns the command manager. This is used to get existing command handlers, in
     * case we have to bolt functionality on top (e.g. tab completion handlers).
     */
    private final JavaPlugin mPlugin;
    
    /**
     * Logger used for outputting warnings and errors which occurred whilst executing a command.
     */
    private final Logger mLogger;

    public CommandManager(JavaPlugin plugin) {
        mCommandMap = new HashMap<String, CommandHandlerRef>();
        mCommandAliasMap = new HashMap<String, String>();
        mCommandObservers = new LinkedList<CommandObserver>();

        mLogger = Logger.getLogger(CommandManager.class.getCanonicalName());
        mPlugin = plugin;
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
            if (commandAnnotation != null) {
                CommandHandler command = (CommandHandler) commandAnnotation;

                CommandHandlerRef handler = mCommandMap.get(command.value());
                if (handler != null) {
                    if (handler.instance.get() != instance) {
                        mLogger.severe("Both a command and the command's tab handler must be defined in the same class instance.");
                        continue;
                    }
                    
                    handler.method = method;
                    handler.console = command.console();
                    handler.remote = command.remote();
                    continue;
                }
                
                for (String alias : command.aliases())
                    mCommandAliasMap.put(alias, command.value());
                
                mCommandMap.put(command.value(), new CommandHandlerRef(instance, method, command.console(), command.remote()));
                for (CommandObserver observer : mCommandObservers)
                    observer.onCommandRegistered(command.value(), command.console(), command.remote());
                
                continue;
            }
            
            final Annotation commandTabAnnotation = method.getAnnotation(CommandCompletionHandler.class);
            if (commandTabAnnotation != null) {
                CommandCompletionHandler completion = (CommandCompletionHandler) commandTabAnnotation;
                
                final PluginCommand pluginCommand = mPlugin.getCommand(completion.value());
                if (pluginCommand == null) {
                    mLogger.severe("Attempted to install a tab handler for /" + completion.value() + ", but the command does not exist.");
                    continue;
                }
                
                pluginCommand.setTabCompleter(this);
                
                CommandHandlerRef handler = mCommandMap.get(completion.value());
                if (handler != null) {
                    if (handler.instance.get() != instance) {
                        mLogger.severe("Both a command and the command's tab handler must be defined in the same class instance.");
                        continue;
                    }
                    
                    handler.autocomplete = method;
                    continue;
                }
                
                mCommandMap.put(completion.value(), new CommandHandlerRef(instance, method));
                continue;
            }
        }
    }
    
    /**
     * Returns the registered command handler for |command|. This method will automatically remove
     * commands from the Command Manager if the handling instance no longer is alive.
     * 
     * @param command   The command for which to find the handler.
     * @return          The command handler if available, otherwise NULL.
     */
    private CommandHandlerRef getCommandHandler(String command) {
        if (mCommandAliasMap.containsKey(command))
            command = mCommandAliasMap.get(command);
        
        final CommandHandlerRef handler = mCommandMap.get(command);
        if (handler == null)
            return null;
        
        final Object instance = handler.instance.get();
        
        // Check whether the instance on which this command was defined is still alive. If it has
        // been garbage collected, then we should unregister this command from Bukkit.
        if (instance == null) {
            if (handler.autocomplete != null) {
                final PluginCommand pluginCommand = mPlugin.getCommand(command);
                if (pluginCommand != null)
                    pluginCommand.setTabCompleter(null);
            }
            
            for (CommandObserver commandObserver : mCommandObservers)
                commandObserver.onCommandRemoved(command);
            
            mCommandMap.remove(command);
            return null;
        }
        
        return handler;
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
        final CommandHandlerRef handler = getCommandHandler(command.getName());
        if (handler == null || handler.method == null)
            return false;
        
        // Check what party executed the command. Mineground supports command senders from three
        // primary sources, namely players (in-game), the console and remote sources.
        if (!(sender instanceof Player)) {
            // We consider RemoteCommandSender as being remote, and all other non-Player senders
            // as the console. Most commands have been implemented following that assumption.
            if (sender instanceof RemoteCommandSender) {
                if (handler.remote == false)
                    return false;

            } else if (handler.console == false) {
                sender.sendMessage("The command /" + command.getName() + " is not available from the console.");
                return true;
            }
        }
        
        // Execute the command by invoking the method, and returning the return value (which should
        // be a boolean) to the caller of onCommand.
        try {
            handler.method.invoke(handler.instance.get(), sender, arguments);
            return true;

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            mLogger.severe("An exception occurred while attempting to execute the command /" + command.getName() + ":");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Called by Bukkit when a player is half-way typing a command, and pressed the <tab> key to
     * finish the value of the last argument they entered. Commands in Mineground may want to
     * implement their own tab-completion procedures to enhance the user experience.
     * 
     * @param sender    The player or console who is executing the command.
     * @param command   The command which |sender| is planning to execute.
     * @param alias     The exact alias used by |sender|.
     * @param arguments Array of arguments which they've entered so far.
     * @return          A sorted list with the strings which could be auto-completed to.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] arguments) {
        final CommandHandlerRef handler = getCommandHandler(command.getName());
        if (handler == null || handler.autocomplete == null)
            return null;
        
        if (!(sender instanceof Player) && handler.console == false)
            return null;
        
        // Execute the tab completion handler. It should return a list of strings (or NULL) which
        // the player can then iterate over to choose which they mean to complete.
        try {
            Object value = handler.autocomplete.invoke(handler.instance.get(), sender, arguments);
            if (!(value instanceof List<?>)) {
                mLogger.severe("The tab completion handler for /" + command.getName() + " must return a list of strings.");
                return null;
            }
            
            return (List<String>) value;

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            mLogger.severe("An exception occurred while attempting to apply tab completion for /" + command.getName() + ":");
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Registers <code>observer</code> as a new command observer with the Command Manager.
     * 
     * @param observer  The command observer to be registered.
     */
    public void registerCommandObserver(CommandObserver observer) {
        mCommandObservers.add(observer);
    }
    
    /**
     * Removes <code>observer</code> from the list of observers in the Command Manager. If the
     * observer has not been registered, it will be silently ignored.
     * 
     * @param observer  The command observer to remove.
     */
    public void removeCommandObserver(CommandObserver observer) {
        mCommandObservers.remove(mCommandObservers);
    }
}
