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

import org.bukkit.command.CommandSender;

import com.mineground.remote.RemoteCommandSender;

public class EntityUtils {
    /**
     * Returns whether <code>sender</code> is a remote command sender, which most likely means that
     * it's someone watching from IRC.
     * 
     * @param sender    The sender to find out for whether it's remote.
     * @return          Whether <code>sender</code> is a remote command sender.
     */
    public static boolean isRemoteCommandSender(CommandSender sender) {
        return sender instanceof RemoteCommandSender;
    }
}
