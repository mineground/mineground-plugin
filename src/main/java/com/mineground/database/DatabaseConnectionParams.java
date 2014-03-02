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

package com.mineground.database;

// Parameters using which the database connection should be established. The values for these fields
// will be read from the Mineground configuration file.
public class DatabaseConnectionParams {
    // Hostname and port of the MySQL server which will be used.
    String hostname;
    int port;
    
    // Username and (optional) password required to establish the connection.
    String username;
    String password;
    
    // The database in which all of Mineground's information is stored.
    String database;
}
