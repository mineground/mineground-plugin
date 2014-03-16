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

import java.util.ArrayList;
import java.util.List;

/**
 * Promises are a tool for convenient asynchronous programming. The problem they solve is similar to
 * Java's native Future object, with the most notable difference being that promises have success
 * (fulfilled) and failure (rejected) methods on a handler object, and don't directly allow the
 * developer to retrieve the value.
 *
 * There are two primary ways of employing a promise: as a separate variable, which will be resolved
 * by another method in your system, or as an immediate return value which will be resolved by an
 * executor handler supplied in the constructor. Examples are as follows.
 *
 * -------------------------------------------------------------------------------------------------
 * Example: Using Promise using external resolving or rejection.
 * -------------------------------------------------------------------------------------------------
 *
 * Promise<String> myPromise = new Promise<String>();
 * myPromise.then(new PromiseResultHandler<String>() {
 *     public void onFulfilled(String result) {
 *         ... the promise has been fulfilled, use |result| to your likings ...
 *     }
 *     public void onRejected(PromiseError error) {
 *         ... the promise has been rejected, check out |error| to see why ...
 *     }
 * });
 * myPromise.resolve("Resolved");
 * myPromise.reject(new PromiseError("Failed"));
 * 
 * -------------------------------------------------------------------------------------------------
 * Example: Using Promise using an executor as a constructor argument.
 * -------------------------------------------------------------------------------------------------
 *
 * new Promise<String>(new PromiseExecutor<String>() {
 *     public void execute(PromiseResult<String> promise) {
 *         ... execute actions required to handle this promise, asynchronously ...
 *
 *         promise.resolve("Resolved");
 *         promise.reject("new PromiseError("Failed"));
 *     }
 * }).then(new PromiseResultHandler<String>() {
 *     public void onFulfilled(String result) {
 *         ... the promise has been fulfilled, use |result| to your likings ...
 *     }
 *     public void onRejected(PromiseError error) {
 *         ... the promise has been rejected, check out |error| to see why ...
 *     }
 * });
 *
 * -------------------------------------------------------------------------------------------------
 *
 * When using the PromiseExecutor, it is important to note that the |execute()| method will be
 * executed synchronously on the same thread as the caller.
 *
 * Promises may only be resolved or rejected once, but can have handlers attached to them both
 * before and after it has been settled. When a new handler gets attached to a settled promise, it
 * will immediately be resolved or rejected based on the original outcome. When a promise gets
 * settled, all current handlers will be settled in order of their attaching.
 *
 * Promises have a convenience method for casting any class to a resolved promise of that type. This
 * could be useful for methods which should return a promise, but have no immediate need to be
 * asynchronous.
 *
 * -------------------------------------------------------------------------------------------------
 * Example: Using Promise.cast() to create an immediately resolved Promise.
 * -------------------------------------------------------------------------------------------------
 *
 * Promise<String> promise = Promise.cast("Hello, world");
 * promise.then(new PromiseResultHandler<String>() {
 *     public void onFulfilled(String result) {
 *         ... |result| will be set to "Hello, world" here. ...
 *     }
 *     public void onRejected(PromiseError error) { }
 * });
 *
 * -------------------------------------------------------------------------------------------------
 *
 * Promises have two additional convenience methods which will come in useful when your code
 * is dealing with multiple promises at the same time. These have slightly interesting semantics.
 *
 * Promise.race([promise1, promise2, ...])
 *
 *     Returns a promise which will be resolved when the first one of the passed promises resolves.
 *     When other promises resolve after that time, their values will be ignored.
 *
 * Promise.all([promise1, promise2, ...])
 *
 *     Returns a promise with a List of the return values, which will be resolved when ALL of the
 *     passed promises resolve. If a passed promise rejects, the returned promise will be rejected
 *     with the passed error as well. If one of the promises never settles, then the returned
 *     Promise will never settle either.
 *
 * -------------------------------------------------------------------------------------------------
 * Example: Using Promise.all() to output "Done" when all calculations have completed.
 * -------------------------------------------------------------------------------------------------
 *
 * List<Promise<Integer>> calculations = ...;
 * Promise.all(calculations).then(new PromiseResultHandler<List<Integer>>() {
 *     public void onFulfilled(Integer result) {
 *         System.out.println("Done! Received " + result.size() + " results.");
 *     }
 *     public void onRejected(PromiseError error) {
 *         ... one of the calculations failed, check |error.reason()| ...
 *     }
 * });
 *
 * -------------------------------------------------------------------------------------------------
 *
 * TODO: Can we somehow allow Promise.then() to return another promise, allowing chaining?
 *
 * @param <SuccessValueType> Type of the argument the Promise must be resolved with.
 * @see <a href="http://www.html5rocks.com/en/tutorials/es6/promises/">JavaScript Promises</a>
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html">Interface Future<V></a>
 */
public class Promise<SuccessValueType> {
    enum PromiseState {
        /**
         * The initial state for a promise which has not been fulfilled or rejected yet.
         */
        Pending,
        
        /**
         * The promise has been fulfilled, and |onFulfilled| will be invoked on all handlers.
         */
        Fulfilled,
        
        /**
         * The promise has been rejected, and |onRejected| will be invoked on all handlers.
         */
        Rejected
    };

    private ArrayList<PromiseResultHandler<SuccessValueType>> mHandlers;
    private PromiseState mState;
    private SuccessValueType mSuccessValue;
    private PromiseError mRejectionError;

    public Promise() {
        mHandlers = new ArrayList<PromiseResultHandler<SuccessValueType>>();
        mState = PromiseState.Pending;
    }

    public Promise(PromiseExecutor<SuccessValueType> executor) {
        this(); // common initialization

        executor.execute(this);
    }

    /**
     * Attaches a new result handler to this promise. If the promise has already been settled, the
     * appropriate method will be immediately invoked on the handler. Otherwise, it will be added
     * to the list of handlers until this promise gets settled.
     *
     * @param handler The result handler which will be notified when the promise resolves.
     */
    public void then(PromiseResultHandler<SuccessValueType> handler) {
        if (mState == PromiseState.Fulfilled) {
            handler.onFulfilled(mSuccessValue);
            return;
        }

        if (mState == PromiseState.Rejected) {
            handler.onRejected(mRejectionError);
            return;
        }

        mHandlers.add(handler);
    }

    /**
     * Resolves this promise with |value|. The promise must not have been previously settled. All
     * attached handlers will immediately have their |onFulfilled| methods invoked.
     *
     * @param value The value the Promise was resolved with.
     * @throws PromiseSettledException When the Promise has already been settled.
     */
    public void resolve(SuccessValueType value) throws PromiseSettledException {
        if (mState != PromiseState.Pending)
            throw new PromiseSettledException();

        mSuccessValue = value;
        mState = PromiseState.Fulfilled;

        for (PromiseResultHandler<SuccessValueType> handler : mHandlers)
            handler.onFulfilled(value);
    }

    /**
     * Rejects this promise because of |error|. The promise must not have been previously settled.
     * All attached handlers will immediately have their |onRejected| methods invoked.
     *
     * @param error The reason the Promise has been rejected.
     * @throws PromiseSettledException When the Promise has already been settled.
     */
    public void reject(PromiseError error) throws PromiseSettledException {
        if (mState != PromiseState.Pending)
            throw new PromiseSettledException();

        mRejectionError = error;
        mState = PromiseState.Rejected;

        for (PromiseResultHandler<SuccessValueType> handler : mHandlers)
            handler.onRejected(error);
    }
    
    /**
     * Rejects this promise because of |error|. The promise must not have been previously settled.
     * All attached handlers will immediately have their |onRejected| methods invoked.
     *
     * @param errorMessage The reason the Promise has been rejected.
     * @throws PromiseSettledException When the Promise has already been settled.
     */
    public void reject(String errorMessage) throws PromiseSettledException {
        reject(new PromiseError(errorMessage));
    }

    /**
     * Casts |value| to a Promise which will immediately be resolved with PromiseCastType as the
     * SuccessValueType. This is a utility function for quick returns in Promise-returning methods.
     *
     * @param value Value which should be casted to a resolved promise.
     * @return      A resolved Promise instance, with |value| as the resolved value.
     */
    public static <PromiseCastType> Promise<PromiseCastType> cast(PromiseCastType value) {
        Promise<PromiseCastType> promise = new Promise<PromiseCastType>();
        promise.resolve(value);
        
        return promise;
    }
    
    /**
     * Returns a Promise which will be resolved once the first once of |promises| has been resolved.
     * We cannot reject the returned promise if either of the |promises| reject, because that would
     * mean that we cannot resolve it anymore once another promise succeeds.
     *
     * @param promises  List of promises which may be resolving soon.
     * @return          A Promise, which will be resolved when the first of the promises resolves.
     */
    public static <SuccessValueType> Promise<SuccessValueType> race(final List<Promise<SuccessValueType>> promises) {
        final Promise<SuccessValueType> promise = new Promise<SuccessValueType>();
        
        PromiseResultHandler<SuccessValueType> handler = new PromiseResultHandler<SuccessValueType>() {
            private int mResolved = 0;
            public void onFulfilled(SuccessValueType result) {
                if (++mResolved == 1)
                    promise.resolve(result);
            }

            public void onRejected(PromiseError error) { }
        };
        
        for (Promise<SuccessValueType> contestant : promises)
            contestant.then(handler);
        
        return promise;
    }
    
    /**
     * Returns a Promise which will be resolved once all of the |promises| have been resolved. If
     * either of the |promises| rejects, the returned promise will also be rejected.
     * 
     * @param promises  List of promises which need to resolve.
     * @return          A Promise, which will be resolved once all passed promises resolve.
     */
    public static <SuccessValueType> Promise<List<SuccessValueType>> all(final List<Promise<SuccessValueType>> promises) {
        final Promise<List<SuccessValueType>> promise = new Promise<List<SuccessValueType>>();
        final int promisesSize = promises.size();
        
        PromiseResultHandler<SuccessValueType> handler = new PromiseResultHandler<SuccessValueType>() {
            private List<SuccessValueType> mValues = new ArrayList<SuccessValueType>();
            public void onFulfilled(SuccessValueType result) {
                mValues.add(result);
                if (mValues.size() == promisesSize)
                    promise.resolve(mValues);
            }

            public void onRejected(PromiseError error) {
                promise.reject(error);
            }
        };
        
        for (Promise<SuccessValueType> includedPromise : promises)
            includedPromise.then(handler);
        
        return promise;
    }
}
