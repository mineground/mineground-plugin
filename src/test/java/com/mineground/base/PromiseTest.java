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

import junit.framework.TestCase;

// Tests for the Promise, PromiseError, PromiseExecutor and PromiseResultHandler classes.
public class PromiseTest extends TestCase {
    // Used for counting how often a promise's result handlers have been invoked.
    private int mPromiseResultCount;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPromiseResultCount = 0;
    }
    
    // Tests that the |Promise.resolve()| method will resolve the promise and invoke the onFulfilled
    // handler with the given |result|.
    public void testSimplePromiseResolve() {
        Promise<String> promise = new Promise<String>();
        promise.then(new PromiseResultHandler<String>() {
            public void onFulfilled(String result) {
                assertEquals("testSimplePromiseResolve", result);
                ++mPromiseResultCount;
            }
            public void onRejected(PromiseError error) {
                fail("PromiseResultHandler::onRejected must not be invoked.");
            }
        });
        
        promise.resolve("testSimplePromiseResolve");
        assertEquals(mPromiseResultCount, 1);
    }
    
    // Tests that the |Promise.reject()| method will reject the promise, and invoke the onRejected
    // handler with |error| as a parameter.
    public void testSimplePromiseReject() {
        Promise<String> promise = new Promise<String>();
        promise.then(new PromiseResultHandler<String>() {
            public void onFulfilled(String result) {
                fail("PromiseResultHandler::onFulfilled must not be invoked.");
            }
            public void onRejected(PromiseError error) {
                assertEquals("testSimplePromiseReject", error.reason());
                ++mPromiseResultCount;
            }
        });
        
        promise.reject(new PromiseError("testSimplePromiseReject"));
        assertEquals(mPromiseResultCount, 1);
    }
    
    // Tests that using a PromiseExecutor as an argument to a Promise's constructor will result in
    // the executor being executed, and it being able to settle the promise appropriately.
    public void testPromiseExecutorConstructor() {
        new Promise<String>(new PromiseExecutor<String>() {
            public void execute(Promise<String> promise) {
                promise.resolve("testPromiseExecutorConstructor");
            }
        }).then(new PromiseResultHandler<String>() {
            public void onFulfilled(String result) {
                assertEquals("testPromiseExecutorConstructor", result);
                ++mPromiseResultCount;
                
            }
            public void onRejected(PromiseError error) {
                fail("PromiseResultHandler::onRejected must not be invoked.");
            }
        });
        
        assertEquals(mPromiseResultCount, 1);
    }
    
    // Tests that a promise with multiple result handlers attached will execute all of them, in the
    // order with which they have been attached to the promise itself.
    public void testMultiplePromiseResultHandlers() {
        Promise<String> promise = new Promise<String>();
        promise.then(new PromiseResultHandler<String>() {
            public void onFulfilled(String result) {
                assertEquals("testMultipleResultHandlers", result);
                assertEquals(0, mPromiseResultCount++);
            }
            public void onRejected(PromiseError error) {
                fail("PromiseResultHandler::onRejected must not be invoked.");
            }
        });
        
        promise.then(new PromiseResultHandler<String>() {
            public void onFulfilled(String result) {
                assertEquals("testMultipleResultHandlers", result);
                assertEquals(1, mPromiseResultCount++);
            }
            public void onRejected(PromiseError error) {
                fail("PromiseResultHandler::onRejected must not be invoked.");
            }
        });

        promise.resolve("testMultipleResultHandlers");
        assertEquals(mPromiseResultCount, 2);
    }

    // Tests that promises which get result handlers attached after they've been settled will still
    // invoke the appropriate methods on the newly attached handlers.
    public void testHandlerAfterPromiseSettled() {
        Promise<String> promise = new Promise<String>();
        promise.then(new PromiseResultHandler<String>() {
            public void onFulfilled(String result) {
                assertEquals("testHandlerAfterPromiseSettled", result);
                assertEquals(0, mPromiseResultCount++);
            }
            public void onRejected(PromiseError error) {
                fail("PromiseResultHandler::onRejected must not be invoked.");
            }
        });

        promise.resolve("testHandlerAfterPromiseSettled");
        promise.then(new PromiseResultHandler<String>() {
            public void onFulfilled(String result) {
                assertEquals("testHandlerAfterPromiseSettled", result);
                assertEquals(1, mPromiseResultCount++);
            }
            public void onRejected(PromiseError error) {
                fail("PromiseResultHandler::onRejected must not be invoked.");
            }
        });

        assertEquals(mPromiseResultCount, 2);
    }
    
    // Tests that attempting to resolve a promise twice will result in an exception being thrown.
    public void testMultipleSettleException() {
        Promise<String> promise = new Promise<String>();
        promise.resolve("testMultipleSettleException");
        
        PromiseSettledException exception = null;
        try {
            promise.resolve("testMultipleSettleException");
        } catch (PromiseSettledException e) {
            exception = e;
        }
        
        assertNotNull(exception);
    }
}
