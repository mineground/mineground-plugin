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

package com.mineground.features;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.account.AccountLevel;
import com.mineground.base.Color;
import com.mineground.base.CommandHandler;
import com.mineground.base.DisconnectReason;
import com.mineground.base.EntityUtils;
import com.mineground.base.FeatureComponent;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Message;
import com.mineground.base.StringUtils;

/**
 * Contains the commands associated with communication: reporting players to administrators, private
 * messaging and similar functionalities. This is a component owned by the CommunicationManager.
 */
public class CommunicationCommands extends FeatureComponent<CommunicationManager> {
    /**
     * Format in which report messages will be presented to online staff.
     */
    private final Message mStaffReportMessage;

    /**
     * Message displayed to a non-staff user when they contacted the online staff.
     */
    private final Message mStaffNotifiedMessage;
    
    /**
     * Message displayed to a non-staff user when sending a message to staff members, while no
     * staff members are currently online on Mineground.
     */
    private final Message mStaffOfflineMessage;
    
    /**
     * Message displayed to a player who has just received a private message from another player.
     */
    private final Message mPrivateMessageReceivedMessage;
    
    /**
     * Message displayed to a player who has just sent a private message to another player.
     */
    private final Message mPrivateMessageSentMessage;
    
    /**
     * Message containing the format in which staff chat will be displayed.
     */
    private final Message mStaffChatMessage;
    
    /**
     * Map between a player and the last person they received a private message from.
     */
    private final Map<Player, String> mLastCommunicationMap;

    public CommunicationCommands(CommunicationManager manager, FeatureInitParams params) {
        super(manager, params);
        
        mStaffReportMessage = Message.Load("report_format");
        mStaffNotifiedMessage = Message.Load("staff_notified");
        mStaffOfflineMessage = Message.Load("no_staff_online");
        mStaffChatMessage = Message.Load("staff_chat_format");
        
        mPrivateMessageReceivedMessage = Message.Load("private_message_received");
        mPrivateMessageSentMessage = Message.Load("private_message_sent");
        
        mLastCommunicationMap = new HashMap<Player, String>();
    }
    
    /**
     * Invoked when a player disconnects from Mineground.
     * 
     * @param player    The player who disconnected from Mineground.
     * @param reason    The reason why they disconnected from the server.
     */
    public void onPlayerDisconnect(Player player, DisconnectReason reason) {
        mLastCommunicationMap.remove(player);
    }
    
    /**
     * The <code>/report</code> command allows players to report other players for misbehavior or
     * other offenses, as well as reporting to the staff that something has happened.
     * 
     * @param sender    The player who is reporting something.
     * @param arguments Words of the message which they're reporting.
     */
    @CommandHandler("report")
    public void onReportCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        if (!player.hasPermission("command.report")) {
            displayCommandError(player, "You don't have permission to report anything to staff.");
            return;
        }

        if (arguments.length == 0) {
            displayCommandUsage(player, "/report [message]");
            return;
        }
        
        List<Player> staff = getAccountManager().getOnlineStaff();
        if (staff.size() > 0) {
            StringBuilder messageBuilder = new StringBuilder();
            for (String argument : arguments)
                messageBuilder.append(argument).append(" ");
            
            // Remove the last space separator which was appended to the message.
            messageBuilder.setLength(messageBuilder.length() - 1);
            
            mStaffReportMessage.setString("nickname", player.getName());
            mStaffReportMessage.setString("message", messageBuilder.toString());
            mStaffReportMessage.send(staff, Color.PLAYER_EVENT);
            
            if (!staff.contains(player)) {
                mStaffNotifiedMessage.send(player, Color.GREEN);
            }
        } else {
            mStaffOfflineMessage.send(player, Color.ACTION_REQUIRED);
        }
        
        // TODO: Send this message to IRC so that folks there can read.
    }
    
    /**
     * Sends a private message from <code>sender</code> to <code>destination</code>.
     * 
     * @param sender        The person to distribute the private message from.
     * @param destination   The person who should receive the message.
     * @param message       The message to be distributed.
     */
    private void sendPrivateMessage(CommandSender sender, CommandSender destination, String message) {
        // TODO: Distribute this message to administrators.
        
        if (!EntityUtils.isRemoteCommandSender(sender)) {
            mPrivateMessageSentMessage.setString("sender", sender.getName());
            mPrivateMessageSentMessage.setString("destination", destination.getName());
            mPrivateMessageSentMessage.setString("message", message);
            mPrivateMessageSentMessage.send(sender, Color.PRIVATE_MESSAGE);
        } else {
            displayCommandSuccess(sender, "Your message has been sent to **" + destination.getName() + "**!");
        }
        
        mPrivateMessageReceivedMessage.setString("sender", sender.getName());
        mPrivateMessageReceivedMessage.setString("destination", destination.getName());
        mPrivateMessageReceivedMessage.setString("message", message);
        mPrivateMessageReceivedMessage.send(destination, Color.PRIVATE_MESSAGE);
        
        // TODO: This should be generalized elsewhere.
        getIrcManager().echoMessage("07PM from 05" + sender.getName() + " 07to 05" + destination.getName() + "07: 05" + message,
                AccountLevel.Moderator);
    }
    
    /**
     * Players can send private messages to one another using the <code>/pm</code> command. Contents
     * of the messages will only be visible to them, the receiver, and administrators (either
     * in-game or when watching on IRC).
     * 
     * @param sender    The player who's sending a private message.
     * @param arguments The message's destination and content.
     */
    @CommandHandler(value = "pm", remote = true)
    public void onPrivateMessageCommand(CommandSender sender, String[] arguments) {
        if (!sender.hasPermission("command.pm")) {
            displayCommandError(sender, "You don't have permission to send a private message.");
            return;
        }
        
        if (arguments.length < 2) {
            displayCommandUsage(sender, "/pm [player] [message]");
            return;
        }

        final Player destination = getServer().getPlayer(arguments[0]);
        if (destination == null) {
            displayCommandError(sender, "No one named **" + arguments[0] + "** is on Mineground now.");
            return;
        }
        
        if (destination == sender) {
            displayCommandError(sender, "You can't send a message to yourself, silly!");
            return;
        }
        
        sendPrivateMessage(sender, destination, StringUtils.join(arguments, " ", 1));
        if (sender instanceof Player)
            mLastCommunicationMap.put((Player) sender, destination.getName());
    }
    
    /**
     * The <code>/r(eply)</code> command allows the player to quickly reply to the last message they
     * received. This command is identical to using <code>/pm</code>, followed by their nickname.
     * 
     * @param sender    The player who is replying to a message.
     * @param arguments The message they're replying with.
     */
    @CommandHandler(value = "reply", aliases = { "r" })
    public void onReplyCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
        if (!player.hasPermission("command.reply")) {
            displayCommandError(player, "You don't have permission to reply to private messages.");
            return;
        }

        if (!mLastCommunicationMap.containsKey(player)) {
            displayCommandError(player, "No one has sent you a message which you can reply to yet.");
            return;
        }
        
        final String nickname = mLastCommunicationMap.get(player);
        final Player destination = getServer().getPlayerExact(nickname);
        
        if (destination == null) {
            displayCommandError(player, "**" + nickname + "** is not connected to Mineground anymore.");
            return;
        }
        
        if (arguments.length == 0) {
            displayCommandUsage(player, "/reply [message]");
            return;
        }
        
        sendPrivateMessage(player, destination, StringUtils.join(arguments));
    }
    
    /**
     * Implements the "msg" (message) command, which is accessible for people communicating with the
     * server remotely. The primary audience for this command are people watching on IRC.
     * 
     * @param sender    The remote sender who wants to say something in-game.
     * @param arguments The message they want to be distributed to players.
     */
    @CommandHandler(value = "msg", ingame = false, console = true, remote = true)
    public void onMessageCommand(CommandSender sender, String[] arguments) {
        if (!sender.hasPermission("command.msg")) {
            displayCommandError(sender, "You don't have permission to display messages in-game.");
            return;
        }
        
        if (arguments.length == 0) {
            displayCommandUsage(sender, "/msg [message]");
            return;
        }
        
        final String message = StringUtils.join(arguments);
        
        // TODO: Even for remote senders, we know whether they are a VIP member, a moderator, an
        //       administrator or a Management member. This should be reflected in the |[IRC]|
        //       prefix their message has. Perhaps we should show |[Admin-IRC]|?
        
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("[IRC] <");
        messageBuilder.append(sender.getName());
        messageBuilder.append("> ");
        messageBuilder.append(message);
        
        final String messageStr = messageBuilder.toString();
        for (Player player : getServer().getOnlinePlayers())
            player.sendMessage(messageStr);
        
        // TODO: Format IRC messages using a Message instance.
        getIrcManager().echoMessage("07" + sender.getName() + ": " + message);
    }
    
    /**
     * Implements the !admin command for IRC, as well as the "admin" command from console and other
     * remote sources. Output will only be visible to other members of the staff.
     * 
     * @param sender    The remote sender who wants to say something to in-game staff.
     * @param arguments The message they want to be distributed to administrators.
     */
    @CommandHandler(value = "admin", ingame = false, console = true, remote = true)
    public void onAdminCommand(CommandSender sender, String[] arguments) {
        if (!sender.hasPermission("command.admin")) {
            displayCommandError(sender, "You don't have permission to send messages to administrators.");
            return;
        }
        
        if (arguments.length == 0) {
            displayCommandUsage(sender, "/admin [message]");
            return;
        }
        
        final String message = StringUtils.join(arguments);
        
        List<Player> staff = getAccountManager().getOnlineStaff();
        if (staff.size() > 0) {
            mStaffChatMessage.setString("nickname", sender.getName());
            mStaffChatMessage.setString("message", message);
            mStaffChatMessage.send(staff, Color.PLAYER_EVENT);
        }
        
        // TODO: Format IRC messages using a Message instance.
        getIrcManager().echoMessage("05*** 07Admin " + sender.getName() + "05: " + message, AccountLevel.Moderator);
    }
}
