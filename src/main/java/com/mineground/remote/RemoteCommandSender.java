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

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;

/**
 * While the primary remote command sender used by Mineground will be users on IRC, for which each
 * user in the IRC channel will get their own instance, it should be possible to extend support for
 * running commands from other external sources, such as an administration channel.
 * 
 * This class generalizes permission handling for the remote command senders, minimizing the API
 * surface each derived class needs to implement.
 */
public abstract class RemoteCommandSender extends PermissibleBase implements CommandSender {
    /**
     * The PermissionBase implementation expects an instance of <code>ServerOperator</code> to act
     * as the broker for whether or not this Permissible should be a server operator. Mineground's
     * implementation will take on that responsibility itself.
     * 
     * Remote Command Senders should <strong>never</strong> be a server operator unless the origin
     * is trusted, and its identity has been validated.
     */
    private static class RemoteCommandSenderServerOperator implements ServerOperator {
        /**
         * Whether the owning RemoteCommandSender should be considered as a Server Operator.
         */
        private boolean mIsOp;
        
        /**
         * Returns whether the owning RemoteCommandSender is a Server Operator on Mineground.
         * 
         * @return Whether owning the RemoteCommandSender is a Server Operator.
         */
        @Override
        public boolean isOp() {
            return mIsOp;
        }

        /**
         * Sets whether the owning RemoteCommandSender is a Server Operator on Mineground.
         * 
         * @param isOp  Whether the owning RemoteCommandSender is a Server Operator.
         */
        @Override
        public void setOp(boolean isOp) {
            mIsOp = isOp;
        }
    }

    /**
     * The Bukkit Server instance which is associated with this RemoteCommandSender.
     */
    private final Server mServer;
    
    public RemoteCommandSender(Server server) {
        super(new RemoteCommandSenderServerOperator());
        mServer = server;
    }

    /**
     * Returns the Bukkit Server instance which is associated with this RemoteCommandSender.
     * 
     * @return Server Bukkit Server instance which is associated with this RemoteCommandSender.
     */
    @Override
    public Server getServer() {
        return mServer;
    }

    /**
     * Default implementation of the multiple-message variant of <code>sendMessage</code>. In this
     * implementation it will map to multiple calls to the singular <code>sendMessage</code>.
     * 
     * @param messages Array of messages which should be send.
     */
    @Override
    public void sendMessage(String[] messages) {
        for (String message : messages)
            sendMessage(message);
    }
}
