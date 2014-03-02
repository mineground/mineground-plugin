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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface FeatureCommand {
    // Name of the command which this method should handle, without a slash.
    String value();
    
    String description() default "";
    String usage() default "";
    
    // List of aliases under which this command should also be executed. This can be used if the
    // command has a long and short representation (e.g. "/reply" and "/r").
    String[] aliases() default { };
    
    // Permission which the player executing this command must have.
    String permission() default "";
    
    // Message which will be shown to players who don't have the permission required to execute this
    // command. The default behavior is to act as if the command does not exist.
    String permissionMessage() default "";
}
