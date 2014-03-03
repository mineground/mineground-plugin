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

// A Waitable is a useful synchronization tool when working with multiple threads, and one thread
// has to wait for on another thread to finish some work. The result of said operation will be
// returned to the calling thread.
public class Waitable<ResultType> {
    // The value with which this Waitable has been signalled.
    private ResultType mValue;
    
    // Whether the Waitable has been signaled, and the get() method can quickly return the value.
    private boolean mSignaled;
    
    // Returns the value of |mValue| once |mSignaled| has been set to TRUE. If the Waitable has not
    // been signaled yet, it will wait indefinitely for the signal to arrive.
    public synchronized ResultType get() throws InterruptedException {
        while (!mSignaled)
            wait();
        
        return mValue;
    }
    
    // Signals the Waitable, making it possible for threads calling get() to retrieve the result.
    public synchronized void signal(ResultType result) {
        mValue = result;
        mSignaled = true;
        
        notifyAll();
    }
}
