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

package com.mineground.remote;

/**
 * Represents a parsed message from IRC, with convenient getters to get the individual parts of the
 * message by name rather than by assumed offset. This class may not be instantiated directly, and
 * instead requires users to call the static IrcMessage::Parse() method. 
 */
public class IrcMessage {
    /**
     * The raw message as the message parser tried to parse it.
     */
    private final String mRawMessage;
    
    /**
     * Type of message which is being stored in this instance.
     */
    private IrcMessageType mType;
    
    /**
     * The origin of this message, e.g. the user or server which sent it.
     */
    private String mOrigin;
    
    /**
     * Text contained in the message itself.
     */
    private String mText;
    
    /**
     * The parser format which will be used to parse the individual components of the message. Only
     * meant for usage in the static Parse method.
     */
    private enum ParserFormat {
        /**
         * Default message format: [origin] [type] [destination] :[text]
         * Messages of type "[type] :[text]" are handled by this parser format as well, but their
         * origin will be set to NULL.
         */
        DEFAULT_PARSER_FORMAT,
    }
    
    /**
     * Parses <code>incomingMessage</code> following RFC 2812, and returns an IrcMessage object
     * containing the parsed message.
     * 
     * @param incomingMessage   The raw message received from an IRC server.
     * @return                  IrcMessage object containing the parsed message.
     */
    public static IrcMessage Parse(String incomingMessage) {
        IrcMessage message = new IrcMessage(incomingMessage);
        ParserFormat format = ParserFormat.DEFAULT_PARSER_FORMAT;

        int messageOffset = incomingMessage.indexOf(' ');

        final String firstWord  = incomingMessage.substring(0, messageOffset);
        switch (firstWord.toUpperCase()) {
            case "PING":
                message.setType(IrcMessageType.PING);
                break;
            case "ERROR":
                message.setType(IrcMessageType.ERROR);
                break;
            default:
                message.setOrigin(textTrim(firstWord));
                
                int typeOffset = messageOffset + 1;
                messageOffset = incomingMessage.indexOf(' ', typeOffset);
                
                final String type = incomingMessage.substring(typeOffset, messageOffset);
                switch (type.toUpperCase()) {
                    case "001":
                        message.setType(IrcMessageType.WELCOME);
                        break;
                    case "376":
                        message.setType(IrcMessageType.MOTD_END);
                        break;

                } // switch(type)
                
                break;

        } // switch(firstWord)
        
        switch(format) {
            case DEFAULT_PARSER_FORMAT:
                message.setText(textTrim(incomingMessage.substring(messageOffset)));
                return message;

        } // switch(format)
        
        return message;
    }
    
    private IrcMessage(String rawMessage) {
        mRawMessage = rawMessage;
        mType = IrcMessageType.UNKNOWN;
    }
    
    /**
     * Returns the raw message contained in this IrcMessage instance.
     * 
     * @return The raw message which has been parsed.
     */
    public String getRawMessage() {
        return mRawMessage;
    }
    
    /**
     * Gets the type of message which this IrcMessage contains. This dictates which member fields
     * of the message object should be used.
     * 
     * @return  The type of message associated with this object.
     */
    public IrcMessageType getType() {
        return mType;
    }
    
    /**
     * Sets the type of message contained in this IrcMessage.
     * 
     * @param type  The type of message this object should be associated with.
     */
    private void setType(IrcMessageType type) {
        mType = type;
    }
    
    /**
     * Returns the origin or the message, which is either the user or the server which send it to
     * this bot. The prepending colon (":") will have been removed.
     *
     * @return The origin of this message.
     */
    public String getOrigin() {
        return mOrigin;
    }
    
    /**
     * Sets the origin of this message. The prepending colon must have been removed at this point.
     * 
     * @param origin The origin of this message.
     */
    private void setOrigin(String origin) {
        mOrigin = origin;
    }
    
    /**
     * 
     * @return
     */
    public String getText() {
        return mText;
    }
    
    /**
     * 
     * @param text
     */
    private void setText(String text) {
        mText = text;
    }
    
    /**
     * Trims <code>input</code> assuming it is an incoming text from IRC. All whitespace will be
     * removed from either end of the string, whereas a starting colon will also be shifted.
     * 
     * @param input The string which should be trimmed according to IRC message rules.
     * @return      String with whitespace and a starting colon removed.
     */
    private static String textTrim(String input) {
        input = input.trim();
        if (input.charAt(0) == ':')
            input = input.substring(1);
        
        return input;
    }
}
