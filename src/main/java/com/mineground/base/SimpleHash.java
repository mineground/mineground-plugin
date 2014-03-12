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
 * SimpleHash is a simpler approach to hashing strings than the more secure SecurePasswordHash.
 * Whereas SecurePasswordHash uses PBKDF2 for hashing, SimpleHash will do a simple hash on the input
 * string returning a 32-bit signed integer.
 * 
 * Do not use the methods on this class for hashing really confidential information, as finding
 * duplicates of values will be significantly easier.
 */
public class SimpleHash {
    /**
     * Calculates a simple hash (32-bit signed integer) based on |text|. The used hash is an
     * algorithm created by Professor Daniel J. Bernstein, DJB hash. It's very efficient, and manual
     * testing shows that there are no duplicate values for strings up to six characters in length.
     * 
     * @param text  The text which should be hashed.
     * @return      The numeric hash calculated based on |text|.
     */
    public static int createHash(String text) {
        return createHash(text.toCharArray());
    }
    
    /**
     * Calculates a simple hash (32-bit signed integer) based on |text|. The used hash is an
     * algorithm created by Professor Daniel J. Bernstein, DJB hash. It's very efficient, and manual
     * testing shows that there are no duplicate values for strings up to six characters in length.
     * 
     * @param text  The text which should be hashed.
     * @return      The numeric hash calculated based on |text|.
     */
    public static int createHash(char[] text) {
        int hash = 5381;
        for (int index = 0; index < text.length; ++index)
            hash = ((hash << 5) + hash) + text[index];
        
        return hash;
    }
}
