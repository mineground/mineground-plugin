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
     * Returns items from <code>items</code>, concatenated as a string with each of the entries
     * divided by the <code>delimiter</code>. Only items in the range of <code>startIndex</code> to
     * <code>endIndex</code> will be included (inclusive).
     * 
     * @param items      Collection of items, some of which should be joined.
     * @param delimiter  The string to insert between the selected <code>items</code>.
     * @param startIndex The first entry in <code>items</code> to be concatenated.
     * @param endIndex   The last entry in <code>items</code> to be concatenated.
     * @return           A string containing the chosen items, separated by <code>delimiter</code>.
     */
    public static String join(Collection<?> items, String delimiter, int startIndex, int endIndex) {
        if (startIndex < 0 || startIndex > items.size() || endIndex > (items.size() - 1))
            return null; // invalid startIndex or endIndex

        StringBuilder builder = new StringBuilder();
        int currentIndex = 0;

        for (Object item : items) {
            if (currentIndex < startIndex) {
                currentIndex++;
                continue;
            }
            
            if (currentIndex++ > endIndex)
                break;
            
            builder.append(item).append(delimiter);
        }
        
        if (builder.length() == 0)
            return "";
        
        builder.setLength(builder.length() - delimiter.length());
        return builder.toString();
    }
    
    /**
     * Returns items from <code>items</code>, concatenated as a string with each of the entries
     * divided by the <code>delimiter</code>. Only items starting at <code>startIndex</code> will be
     * included in the joined string (inclusive).
     * 
     * @param items      Collection of items, some of which should be joined.
     * @param delimiter  The string to insert between the selected <code>items</code>.
     * @param startIndex The first entry in <code>items</code> to be concatenated.
     * @return           A string containing the chosen items, separated by <code>delimiter</code>.
     */
    public static String join(Collection<?> items, String delimiter, int startIndex) {
        return join(items, delimiter, startIndex, items.size() - 1);
    }
    
    /**
     * Returns all <code>items</code>, concatenated as a string with each of the entries divided
     * by the <code>delimiter</code>.
     * 
     * @param items     Collection of items which should be joined.
     * @param delimiter The string to insert between the <code>items</code>.
     * @return          A string containing all the items, separated by <code>delimiter</code>.
     */
    public static String join(Collection<?> items, String delimiter) {
        return join(items, delimiter, 0, items.size());
    }
    
    /**
     * Returns all <code>items</code>, concatenated as a string.
     * 
     * @param items     Collection of items which should be joined.
     * @return          A string containing all the items, separated by a space.
     */
    public static String join(Collection<?> items) {
        return join(items, " ", 0, items.size());
    }
    
    /**
     * Returns items from <code>items</code>, concatenated as a string with each of the entries
     * divided by the <code>delimiter</code>. Only items in the range of <code>startIndex</code> to
     * <code>endIndex</code> will be included (inclusive).
     * 
     * @param items      Collection of items, some of which should be joined.
     * @param delimiter  The string to insert between the selected <code>items</code>.
     * @param startIndex The first entry in <code>items</code> to be concatenated.
     * @param endIndex   The last entry in <code>items</code> to be concatenated.
     * @return           A string containing the chosen items, separated by <code>delimiter</code>.
     */
    public static String join(Object[] items, String delimiter, int startIndex, int endIndex) {
        return join(Arrays.asList(items), delimiter, startIndex, endIndex);
    }
    
    /**
     * Returns items from <code>items</code>, concatenated as a string with each of the entries
     * divided by the <code>delimiter</code>. Only items starting at <code>startIndex</code> will be
     * included in the joined string (inclusive).
     * 
     * @param items      Collection of items, some of which should be joined.
     * @param delimiter  The string to insert between the selected <code>items</code>.
     * @param startIndex The first entry in <code>items</code> to be concatenated.
     * @return           A string containing the chosen items, separated by <code>delimiter</code>.
     */
    public static String join(Object[] items, String delimiter, int startIndex) {
        return join(Arrays.asList(items), delimiter, startIndex, items.length - 1);
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
        return join(Arrays.asList(items), delimiter, 0, items.length - 1);
    }
    
    /**
     * Returns all <code>items</code>, concatenated as a string.
     * 
     * @param items     Collection of items which should be joined.
     * @return          A string containing all the items, separated by a space.
     */
    public static String join(Object[] items) {
        return join(Arrays.asList(items), " ", 0, items.length - 1);
    }
}
