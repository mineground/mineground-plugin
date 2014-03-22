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

import java.util.Arrays;
import java.util.Collection;

/**
 * Utility methods for dealing with strings.
 */
public class StringUtils {
    /**
     * Returns all <code>items</code>, concatenated as a string with each of the entries divided
     * by the <code>delimiter</code>.
     * 
     * @param items     Collection of items which should be joined.
     * @param delimiter The string to insert between the <code>items</code>.
     * @return          A string containing all the items, separated by <code>delimiter</code>.
     */
    public static String join(Collection<?> items, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (Object item : items)
            builder.append(item).append(delimiter);
        
        builder.setLength(builder.length() - delimiter.length());
        return builder.toString();
    }
    
    /**
     * Returns all <code>items</code>, concatenated as a string.
     * 
     * @param items     Collection of items which should be joined.
     * @return          A string containing all the items, separated by a space.
     */
    public static String join(Collection<?> items) {
        return join(items, " ");
    }
    
    /**
     * Returns all <code>items</code>, concatenated as a string with each of the entries divided
     * by the <code>delimiter</code>.
     * 
     * @param items     Collection of items which should be joined.
     * @param delimiter The string to insert between the <code>items</code>.
     * @return          A string containing all the items, separated by <code>delimiter</code>.
     */
    public static String join(Object[] items, String delimiter) {
        return join(Arrays.asList(items), delimiter);
    }
    
    /**
     * Returns all <code>items</code>, concatenated as a string.
     * 
     * @param items     Collection of items which should be joined.
     * @return          A string containing all the items, separated by a space.
     */
    public static String join(Object[] items) {
        return join(Arrays.asList(items), " ");
    }
}
