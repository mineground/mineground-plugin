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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

/**
 * Represents a message, to be send to a player, as it has been defined in the configuration. Macros
 * are available for referring to variables in the messages. The main benefit of declaring messages
 * in the configuration file is ease of editing, and that the code doesn't have to care about the
 * message's format, number of lines, and so on.
 */
public class Message {
    /**
     * A list of the individual lines this message consists of.
     */
    private final List<String> mMessageLines;
    
    /**
     * A key -> value map containing the to-be-replaced macros in this message.
     */
    private final Map<String, String> mMessageMacros;
    
    /**
     * The Mineground configuration directives. Messages will be loaded from the "messages"
     * namespace in the YAML file, with the key being the message's name.
     */
    private static Configuration sConfiguration;
    
    /**
     * Initializes a Message object based on |messageName| from the configuration.
     * 
     * @param messageName   Name of the message to load from the configuration.
     * @return              Message object for working with the message.
     */
    public static Message Load(String messageName) {
        final ArrayList<String> messageLines = new ArrayList<String>();
        final String canonicalMessageName = "messages." + messageName;
        
        if (sConfiguration.isList(canonicalMessageName)) {
            for (String messageLine : sConfiguration.getStringList(canonicalMessageName))
                messageLines.add(messageLine);
        } else if (sConfiguration.isString(canonicalMessageName)) {
            messageLines.add(sConfiguration.getString(canonicalMessageName));
        } else {
            Logger.getLogger(Message.class.getCanonicalName()).severe("The message \"" + messageName + "\" does not exist.");
            messageLines.add("[UNKNOWN MESSAGE: " + messageName + "]");
        }
        
        return new Message(messageLines);
    }
    
    private Message(List<String> messageLines) {
        mMessageLines = messageLines;
        mMessageMacros = new HashMap<String, String>();
    }
    
    /**
     * Creates a macro for {KEY} to be replaced with VALUE.
     * 
     * @param key   The key of the macro, which will be replaced.
     * @param value The value (type: long) it should be replaced with.
     * @return      This object, allowing call chaining.
     */
    public Message setInteger(String key, long value) {
        mMessageMacros.put(key, String.valueOf(value));
        return this;
    }
    
    /**
     * Creates a macro for {KEY} to be replaced with VALUE.
     * 
     * @param key   The key of the macro, which will be replaced.
     * @param value The value (type: double) it should be replaced with.
     * @return      This object, allowing call chaining.
     */
    public Message setDouble(String key, double value) {
        mMessageMacros.put(key, String.valueOf(value));
        return this;
    }
    
    /**
     * Creates a macro for {KEY} to be replaced with VALUE.
     * 
     * @param key   The key of the macro, which will be replaced.
     * @param value The value (type: string) it should be replaced with.
     * @return      This object, allowing call chaining.
     */
    public Message setString(String key, String value) {
        mMessageMacros.put(key, value);
        return this;
    }
    
    /**
     * Sends the message to the indicated CommandSender. CommandSender is an interface implemented
     * by both the Player and the various console objects, allowing Messages to be used for any kind
     * of sender in commands.
     * 
     * @param destination   The player or console to send this message to.
     * @param color         Base color of the message to send.
     */
    public void send(CommandSender destination, String color) {
        final List<String> messageLines = compileMessage(color);
        for (String messageLine : messageLines)
            destination.sendMessage(messageLine);
    }
    
    /**
     * Sends the message to the an array of players or consoles. CommandSender is an interface
     * implemented by both the Player and the various console objects, allowing Messages to be used
     * for any kind of sender in commands.
     * 
     * @param destinationArray  Array of players to send this message to.
     * @param color             Base color of the message to send.
     */
    public void send(CommandSender[] destinationArray, String color) {
        final List<String> messageLines = compileMessage(color);
        for (String messageLine : messageLines) {
            for (CommandSender destination : destinationArray)
                destination.sendMessage(messageLine);
        }
    }
    
    /**
     * Sends the message to a list of command senders.
     * 
     * @param destinationList   List of players to send this message to.
     * @param color             Base color of the message to send.
     */
    public void send(Iterable<? extends CommandSender> destinationList, String color) {
        final List<String> messageLines = compileMessage(color);
        for (String messageLine : messageLines) {
            for (CommandSender destination : destinationList)
                destination.sendMessage(messageLine);
        }
    }
    
    /**
     * Compiles the actual message which is about to be distributed. For each line that's part of
     * this Message, fill in all the Macros and prepend messages with the intended color.
     * 
     * @param color Base color of the message, which will be prepended to each message line.
     * @return      A list containing each complete message line.
     */
    private List<String> compileMessage(String color) {
        final ArrayList<String> messageLines = new ArrayList<String>(mMessageLines.size());
        for (String messageLine : mMessageLines) {
            for (String macroKey : mMessageMacros.keySet())
                messageLine = messageLine.replace("{" + macroKey + "}", mMessageMacros.get(macroKey));
            
            // TODO: Do we really need to fix silly YAML-file encoding issues here? :-(.
            messageLine = messageLine.replace("\u00C2", "");
            
            messageLines.add(color + messageLine);
        }
        
        return messageLines;
    }
    
    /**
     * Sets the configuration instance which should be used for loading messages. This method should
     * only be called from the Mineground class, during plugin loading or unloading, or for testing.
     *
     * @param configuration The Configuration object used for Mineground.
     */
    public static void SetConfiguration(Configuration configuration) {
        sConfiguration = configuration;
    }
}
