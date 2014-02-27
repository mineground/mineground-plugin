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

// Interface which declares the |execute| method that will be invoked when using a Promise's
// Executor constructor. The executor can interact with the promise through the |promise| argument,
// removing the need for a final instance of the promise outside of the executor's scope.
public interface PromiseExecutor<SuccessValueType> {
    public void execute(Promise<SuccessValueType> promise);
}
