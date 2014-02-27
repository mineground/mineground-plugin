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

// The PromiseError class contains information about why a promise has failed. More advanced systems
// may choose to specialize this class to contain additional information.
//
// TODO: Should we attach a stack trace to PromiseError objects?
public class PromiseError {
    private String mReason;

    // Initializes the PromiseError object, giving |reason| as the cause of the rejection.
    public PromiseError(String reason) {
        mReason = reason;
    }

    // Returns the reason the promise has been rejected.
    public String reason() {
        return mReason;
    }
}
