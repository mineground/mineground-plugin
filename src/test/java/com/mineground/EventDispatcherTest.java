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

package com.mineground;

import java.lang.ref.WeakReference;

import com.mineground.base.Feature;
import com.mineground.base.FeatureTestBase;

import junit.framework.TestCase;

// Tests for the EventDispatcher class, which is responsible for dispatching events from Bukkit
// to the rest of the plugin. This is critical functionality.
public class EventDispatcherTest extends TestCase {
    // Instance of the Event Dispatcher to be used for testing. This will be reset just before each
    // ran test, to ensure a clean local state for the dispatcher.
    private EventDispatcher mEventDispatcher;
    
    // Used to count the number of times certain methods have been invoked.
    private int mInvocationCount;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mEventDispatcher = new EventDispatcher();
        mInvocationCount = 0;
    }
    
    // Tests that basic registration works, and that upon dispatching an event the listeners will
    // be invoked as expected, and in the order they should be invoked in.
    public void testBasicRegistrationAndInvokation() {
        Feature myFeature = new FeatureTestBase() {
            public void onMinegroundLoaded() {
                assertEquals(0, mInvocationCount++);
            }
            
            public void onMinegroundUnloaded() {
                assertEquals(1, mInvocationCount++);
            }
        };

        mEventDispatcher.registerFeature(myFeature);
        mEventDispatcher.onMinegroundLoaded();
        mEventDispatcher.onMinegroundUnloaded();
        
        assertEquals(2, mInvocationCount);
    }
    
    // Tests that events will not be invoked if the instance on which they were defined went away.
    // Since the EventDispatcher only relays information, there is no need for it to have a strong
    // reference to the active Feature instances.
    public void testFeatureLifetimeIsWeak() {
        Feature myFeature = new FeatureTestBase() {
            public void onMinegroundLoaded() {
                assertEquals(0, mInvocationCount++);
            }
            
            public void onMinegroundUnloaded() {
                fail("onMinegroundUnloaded must not be invoked after losing strong references.");
            }
        };

        mEventDispatcher.registerFeature(myFeature);
        mEventDispatcher.onMinegroundLoaded();
        
        WeakReference<Feature> reference = new WeakReference<Feature>(myFeature);

        myFeature = null; // lose the only strong reference to the feature.
        while (reference.get() != null)
            System.gc();
        
        mEventDispatcher.onMinegroundUnloaded();
    }
}
