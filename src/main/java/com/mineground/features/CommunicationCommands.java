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

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mineground.base.Color;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureComponent;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Message;

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

    public CommunicationCommands(CommunicationManager manager, FeatureInitParams params) {
        super(manager, params);
        
        mStaffReportMessage = Message.Load("report_format");
        mStaffNotifiedMessage = Message.Load("staff_notified");
        mStaffOfflineMessage = Message.Load("no_staff_online");
    }
    
    /**
     * The /report command allows players to report other players for misbehavior or other offenses,
     * as well as reporting to the staff that something has happened.
     * 
     * @param sender    The player who is reporting something.
     * @param arguments Words of the message which they're reporting.
     */
    @CommandHandler("report")
    public void onReportCommand(CommandSender sender, String[] arguments) {
        final Player player = (Player) sender;
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
}
