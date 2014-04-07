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

/**
 * Methods implementing support for a command must be annotated with the @CommandHandler interface
 * to identify the command they're handling, as well as optionally setting options such as whether
 * the command may be executed from the console.
 * 
 * The CommandManager will search for methods annotated with @CommandHandler before it adds them to
 * a map of all supported Mineground commands.
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface CommandHandler {
    /**
     * Name of the command which this method should handle, without a slash.
     *
     * @return The name of this command.
     */
    String value();
    
    /**
     * Returns a list of aliases which this command handler is also responsible for. Aliases will
     * only be available when executing a command from either in-game or from the command line.
     * 
     * TODO: Should we also make aliases available for remote commands, e.g. IRC?
     * 
     * @return A list of aliases this command should also listen to.
     */
    String[] aliases() default {};
    
    /**
     * Whether this command may be executed by players in-game. This is useful when certain commands
     * ("!msg" being the prime example) don't make sense to be executed in-game.
     * 
     * @return Whether this command may be executed by in-game players.
     */
    boolean ingame() default true;
    
    /**
     * Whether this command may be executed from the console. Because most commands won't make sense
     * to be executed on the console, this defaults to false.
     *
     * @return Whether this command may be executed from the console.
     */
    boolean console() default false;
    
    /**
     * Whether this command may be executed from remote sources, which will expose it in a source-
     * specific syntax. In case of IRC, a command named "foo" will be made available on IRC as
     * "!foo", whereas the IrcManager will be responsible for curating appropriate permissions.
     */
    boolean remote() default false;
}
