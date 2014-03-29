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
 * Enumeration containing the message types which the IrcMessage parser can parse.
 */
public enum IrcMessageType {
    /**
     * 001 - First message following client registration.
     */
    WELCOME,
    
    /**
     * 376 - Message Of The Day (end).
     */
    MOTD_END,
    
    /**
     * PING - Request from the server to send a keep-alive message.
     */
    PING,
    
    /**
     * ERROR - Serious (often fatal) error message from the server.
     */
    ERROR,
    
    /**
     * Unknown message, unable to parse. Should be defined last.
     */
    UNKNOWN
}
