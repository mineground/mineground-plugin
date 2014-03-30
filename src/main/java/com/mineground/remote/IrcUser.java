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

import com.mineground.remote.IrcMessage.Origin;

/**
 * 
 * 
 */
public class IrcUser extends RemoteCommandSender {
    private IrcConnection mConnection;
    private Origin mOrigin;
    
    public IrcUser(Server server, IrcConnection connection, Origin origin) {
        super(server);
        
        mConnection = connection;
        mOrigin = origin;
    }
    
    public void updateOriginIfNeeded(Origin origin) {
        // We should probably take a more granular approach than just overwriting existing values..
        mOrigin = origin;
    }

    @Override
    public String getName() {
        return mOrigin.getNickname();
    }

    @Override
    public void sendMessage(String message) {
        mConnection.send("PRIVMSG #Mineground :" + mOrigin.getNickname() + ": " + message);
    }
}
