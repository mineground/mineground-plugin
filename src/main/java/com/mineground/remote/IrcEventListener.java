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

/**
 * The IrcManager listens to incoming events from IRC through a limited set of callbacks. These are
 * more primitive versions of the events received from the connection on the IRC thread.
 */
public interface IrcEventListener {
    /**
     * Event which will be invoked when a message has been received from another user on IRC. The
     * <code>channel</code> parameter can be identical to <code>nickname</code> in case this is a
     * private message directly to the connected bot. This event will be invoked on the main thread.
     * 
     * @param nickname  Nickname of the person who sent a message.
     * @param channel   Channel they sent it to, or their nickname for a private message.
     * @param message   Message which they sent.
     */
    public void onMessageReceived(String nickname, String channel, String message);
}
