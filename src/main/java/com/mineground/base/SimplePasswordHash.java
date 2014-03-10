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

/**
 * SimplePasswordHash is a simpler approach to hashing passwords compared to SecurePasswordHash.
 * Whereas SecurePasswordHash uses PBKDF2 for hashing, SimplePasswordHash will do a simple hash on
 * the input string returning a 32-bit signed integer.
 * 
 * Do not use the methods on this class for hashing really confidential passwords, as finding
 * duplicates of values will be significantly easier. This class has been designed to have a similar
 * API to SecurePasswordHash, but returns integers as the hashes instead of strings.
 */
public class SimplePasswordHash {

    public static int createHash(String password) {
        return createHash(password.toCharArray());
    }
    
    public static int createHash(char[] password) {
        return -1;
    }
    
    public static boolean validatePassword(String password, int correctHash) {
        return validatePassword(password.toCharArray(), correctHash);
    }
    
    public static boolean validatePassword(char[] password, int correctHash) {
        return createHash(password) == correctHash;
    }
}
