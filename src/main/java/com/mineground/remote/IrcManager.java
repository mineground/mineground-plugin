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

package com.mineground.remote;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.mineground.CommandManager;
import com.mineground.CommandObserver;
import com.mineground.account.AccountLevel;
import com.mineground.remote.IrcConnection.ConnectionParams;

/**
 * The IRC Manager is responsible for maintaining a connection with the IRC server when enabled
 * through Mineground's configuration. It also curates a list of users in the primary IRC channel,
 * as well as their status and assumed Mineground permission level.
 */
public class IrcManager implements CommandObserver, IrcEventListener {
    /**
     * Prefix to use when triggering Mineground commands from IRC. This will be used instead of the
     * slash characters ("/") in-game, as well as the prefixless console.
     */
    private final static String IRC_COMMAND_PREFIX = ".";
    
    /**
     * A set containing all the commands which can be handled from IRC. The Command Manager will
     * inform us when a command gets registered or goes away.
     */
    private final Set<String> mCommands;
    
    /**
     * Instance of Mineground's Command Manager. We execute commands through Mineground's normal
     * execution system, using an IrcUser instance as the command's sender.
     */
    private final CommandManager mCommandManager;
    
    /**
     * The actual connection with the IRC server.
     */
    private final IrcConnection mConnection;
    
    /**
     * The main Mineground plugin instance as a generalized JavaPlugin. Command senders need to be
     * aware of the Server they're associated with, and permission attachments must be owned by
     * a Plugin instance so that they can be appropriately discarded of.
     */
    private final JavaPlugin mPlugin;
    
    /**
     * Task Id of the running repeating task within Bukkit's task scheduler. This is used to poll
     * for incoming messages from the IRC thread on the main server thread.
     */
    private int mSchedulerTaskId;
    
    public IrcManager(Configuration configuration, CommandManager commandManager, JavaPlugin plugin) {
        mCommands = new HashSet<String>();
        mCommandManager = commandManager;
        mPlugin = plugin;
        
        ConnectionParams connectionParams = new ConnectionParams();
        connectionParams.nickname = configuration.getString("irc.nickname", "MinegroundDev");
        connectionParams.password = configuration.getString("irc.password", "");
        connectionParams.servers = configuration.getStringList("irc.servers");
        connectionParams.channels = configuration.getStringList("irc.channels");
        
        mConnection = new IrcConnection(connectionParams, mPlugin.getServer());
        mConnection.addListener(this);
        mConnection.connect();
        
        // Register ourselves as a command observer, so that we get informed about created commands.
        mCommandManager.registerCommandObserver(this);
        
        // Poll for incoming messages from the IRC thread every three ticks, which will be roughly
        // equal to once per ((1000 / 20) * 3 =) 150 milliseconds, depending on server load.
        mSchedulerTaskId = getScheduler().scheduleSyncRepeatingTask(mPlugin, new Runnable() {
            public void run() {
                if (mConnection != null)
                    mConnection.doPollForMessages();
            }
        }, 2, 3);
    }
    
    /**
     * Distributes <code>message</code> to the configured IRC echo channel. <code>level</code> is
     * used to select the audience which will be receiving this message on IRC.
     * 
     * @param message   The message which should be echo'ed to IRC.
     * @param level     Required level in order to be able to read the message.
     */
    public void echoMessage(String message, AccountLevel level) {
        if (mConnection == null)
            return;
        
        String prefix = "";
        switch (level) {
            case Management:
                prefix = "&";
                break;
            case Administrator:
                prefix = "@";
                break;
            case Moderator:
                prefix = "%";
                break;
            case VIP:
                prefix = "+";
                break;
        }

        mConnection.send("PRIVMSG " + prefix + "#Mineground :" + message);
    }
    
    /**
     * Distributes <code>message</code> to the configured IRC echo channel.
     * 
     * @param message   The message which should be echo'ed to IRC.
     */
    public void echoMessage(String message) {
        echoMessage(message, AccountLevel.Guest);
    }
    
    /**
     * Disconnects the connection with IRC if it has been established. It's important that we shut
     * down the connection in a clean way when the module is being unloaded.
     */
    public void disconnect() {
        echoMessage("04*** The Mineground plugin is being unloaded.");
        
        getScheduler().cancelTask(mSchedulerTaskId);
        mSchedulerTaskId = -1;
        
        mConnection.disconnect();
    }
    
    /**
     * Executes <code>command</code> within Mineground, with <code>user</code> being the person who
     * has executed the command, and <code>arguments</code> as the command's arguments. This will
     * call through to the common Command Manager on Mineground.
     * 
     * @param user      IRC user who has executed the command.
     * @param command   Name of the command which they're executing.
     * @param arguments Further arguments passed on to the command.
     * @return          Whether the command could be routed to a command handler.
     */
    private boolean executeCommand(IrcUser user, String command, String[] arguments) {
        PluginCommand pluginCommand = mPlugin.getServer().getPluginCommand(command);
        if (pluginCommand.getPlugin() != mPlugin)
            return false;  // only execute commands owned by Mineground.
        
        try {
            // Exceptions occurring in commands should never crash the server thread, or even break
            // the code flow by throwing an exception there.
            return mCommandManager.onCommand(user, pluginCommand, arguments);
            
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }
    
    /**
     * Invoked on the server thread when a message has been received from IRC. Always output the
     * message as a means of logging, but then discard it unless <code>nickname</code> is trying to
     * execute a recognized command.
     * 
     * @param user          The user who has sent this message.
     * @param destination   Channel they sent it to, or their nickname for a private message.
     * @param message       Message which they sent.
     */
    @Override
    public void onMessageReceived(IrcUser user, String destination, String message) {
        if (!message.startsWith(IRC_COMMAND_PREFIX))
            return;  // this message isn't executing a command.
        
        // TODO: Move this behavior to the IrcUser class instead.
        if (!user.hasPermission("mineground.builder"))
            user.addAttachment(mPlugin, "mineground.builder", true);

        final String[] parts = message.split(" ", 2);
        final String command = parts[0].substring(IRC_COMMAND_PREFIX.length());

        if (!mCommands.contains(command))
            return;  // the command this message wants to execute doesn't exist.
        
        // Create an array with the arguments. If |message| contains a space, |arguments| will be
        // an array with each separate word in that range. Otherwise a new empty array will be used.
        final String[] arguments = parts.length == 2 ? parts[1].split("\\s+") : new String[0];
        
        // Route the command using the Command Manager to a method, and execute it.
        executeCommand(user, command, arguments);
    }

    /**
     * Invoked by the Command Manager when a new command gets registered. When <code>remote</code>
     * has been enabled for this command, we'll call through to the handler whenever anyone on IRC
     * executes the !<code>name</code> command.
     * 
     * @param name      Name of the command which has been created.
     * @param ingame    Whether this command can be executed from in-game.
     * @param console   Whether this command can be executed from the console.
     * @param remote    Whether this command can be executed from remote sources.
     */
    @Override
    public void onCommandRegistered(String name, boolean ingame, boolean console, boolean remote) {
        if (!remote)
            return;
        
        mCommands.add(name);
    }

    /**
     * Invoked by the Command Manager when an existing command gets removed. If an IRC command named
     * !<code>name</code> was known to the IRC Manager, it will be unavailable afterwards.
     * 
     * @param name  Name of the command which has been removed.
     */
    @Override
    public void onCommandRemoved(String name) {
        mCommands.remove(name);
    }
    
    /**
     * Returns the Bukkit scheduler from |mPlugin|. Convenience method to make the code needing this
     * more readable, since it's a long call-chain.
     *
     * @return Instance of Bukkit's task scheduler.
     */
    private BukkitScheduler getScheduler() { return mPlugin.getServer().getScheduler(); }
}
