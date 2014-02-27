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

// Handler interface which users of |Promise.then()| need to implement in order to know whether
// the promise has been fulfilled or rejected. The generic parameter |SuccessValueType| indicates
// the argument type which a promise must be resolved with.
public interface PromiseResultHandler<SuccessValueType> {
    // Called when a promise has been fulfilled, with |result| of type |SuccessValueType| containing
    // the information retrieved by the resolver.
    void onFulfilled(SuccessValueType result);

    // Called when a promise has been rejected, with |error| being the PromiseError object
    // containing information about why it has been rejected.
    void onRejected(PromiseError error);
}
