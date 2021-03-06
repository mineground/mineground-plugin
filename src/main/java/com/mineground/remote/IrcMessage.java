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
    private Origin mOrigin;
    
    /**
     * The destination of the message, which could be a user or a channel.
     */
    private String mDestination;
    
    /**
     * Text contained in the message itself.
     */
    private String mText;
    
    /**
     * Represents the information we know about a user's origin. These come in two primary formats,
     * namely nick!ident@host for users, or fully qualified names for servers.
     */
    public static class Origin {
        private String mNickname;
        
        public Origin(String origin) {
            if (origin.indexOf('!') != -1)
                mNickname = origin.substring(0, origin.indexOf('!'));
            else
                mNickname = origin;
        }
        
        public String getNickname() {
            return mNickname;
        }
    }
    
    /**
     * The parser format which will be used to parse the individual components of the message. Only
     * meant for usage in the static Parse method.
     */
    private enum ParserFormat {
        /**
         * Default message format: [origin] [type] [destination] :[text]
         */
        DEFAULT_PARSER_FORMAT,
        
        /**
         * Simple message format: [type] :[text]
         */
        SIMPLE_PARSER_FORMAT
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
                format = ParserFormat.SIMPLE_PARSER_FORMAT;
                break;
            case "ERROR":
                message.setType(IrcMessageType.ERROR);
                format = ParserFormat.SIMPLE_PARSER_FORMAT;
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
                    case "433":
                        message.setType(IrcMessageType.NICKNAME_IN_USE);
                        break;
                    case "NOTICE":
                        message.setType(IrcMessageType.NOTICE);
                        break;
                    case "PRIVMSG":
                        message.setType(IrcMessageType.PRIVMSG);
                        break;
                        
                } // switch(type)
                
                break;

        } // switch(firstWord)
        
        switch(format) {
            case DEFAULT_PARSER_FORMAT:
                int destinationOffset = messageOffset + 1;
                
                messageOffset = incomingMessage.indexOf(' ', destinationOffset);
                if (messageOffset == -1) {
                    message.setDestination(incomingMessage.substring(destinationOffset));
                    message.setText("");
                } else {
                    message.setDestination(textTrim(incomingMessage.substring(destinationOffset, messageOffset)));
                    message.setText(textTrim(incomingMessage.substring(messageOffset)));
                }

                return message;

            case SIMPLE_PARSER_FORMAT:
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
    public Origin getOrigin() {
        return mOrigin;
    }
    
    /**
     * Sets the origin of this message. The prepending colon must have been removed at this point.
     * 
     * @param origin The origin of this message.
     */
    private void setOrigin(String origin) {
        mOrigin = new Origin(origin);
    }
    
    /**
     * Returns the destination of this message. This could be either a user or a channel name.
     * 
     * @return The destination of this message.
     */
    public String getDestination() {
        return mDestination;
    }
    
    /**
     * Sets the destination of this this message.
     * 
     * @param destination The destination of this message.
     */
    private void setDestination(String destination) {
        mDestination = destination;
    }
    
    /**
     * Returns the textual contents of this message, with the origin and message type stripped out.
     * 
     * @return The text contained in this message.
     */
    public String getText() {
        return mText;
    }
    
    /**
     * Sets the text contained in this message. It should already have been trimmed, and any leading
     * colon (":") should have been removed.
     * 
     * @param text The text contained in this message.
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
