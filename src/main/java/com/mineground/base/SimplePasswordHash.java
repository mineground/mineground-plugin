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
    /**
     * Calculates a simple hash (32-bit signed integer) based on |password|. The used hash is an
     * algorithm created by Professor Daniel J. Bernstein, DJB hash. It's very efficient, and manual
     * testing shows that there are no duplicate values for strings up to six characters in length.
     * 
     * @param password  The password which should be hashed.
     * @return          The numeric hash calculated based on |password|.
     */
    public static long createHash(String password) {
        return createHash(password.toCharArray());
    }
    
    /**
     * Calculates a simple hash (32-bit signed integer) based on |password|. The used hash is an
     * algorithm created by Professor Daniel J. Bernstein, DJB hash. It's very efficient, and manual
     * testing shows that there are no duplicate values for strings up to six characters in length.
     * 
     * @param password  The password which should be hashed.
     * @return          The numeric hash calculated based on |password|.
     */
    public static long createHash(char[] password) {
        long hash = 5381;
        for (int index = 0; index < password.length; ++index)
            hash = ((hash << 5) + hash) + password[index];
        
        return hash;
    }
    
    /**
     * Verifies that the hash of |password| is |correctHash|, thereby validating that the given
     * password is the correct one.
     * 
     * @param password      The password which should be validated.
     * @param correctHash   The hash against which the password should be validated.
     * @return              Is |password| the correct password?
     */
    public static boolean validatePassword(String password, int correctHash) {
        return validatePassword(password.toCharArray(), correctHash);
    }
    
    /**
     * Verifies that the hash of |password| is |correctHash|, thereby validating that the given
     * password is the correct one.
     * 
     * @param password      The password which should be validated.
     * @param correctHash   The hash against which the password should be validated.
     * @return              Is |password| the correct password?
     */
    public static boolean validatePassword(char[] password, int correctHash) {
        return createHash(password) == correctHash;
    }
}
