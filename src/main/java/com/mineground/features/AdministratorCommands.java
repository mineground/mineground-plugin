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

import com.mineground.account.PlayerLog;
import com.mineground.account.PlayerLog.Note;
import com.mineground.account.PlayerLog.NoteType;
import com.mineground.base.CommandHandler;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
import com.mineground.base.StringUtils;

/**
 * Administrators need various functionalities in order to effectively administer the server. Among
 * these are punishment commands, diagnostic commands and similar functionalities.
 * 
 * Mind that this is not a dumping place for any command that is only applicable for administrators.
 */
public class AdministratorCommands extends FeatureBase {
    public AdministratorCommands(FeatureInitParams params) { super(params); }

    /**
     * Command for writing a new node to a player's account. This implementation is usable from both
     * in-game, the console and from IRC.
     * 
     * @param sender    The player, console or user wanting to add a note.
     * @param arguments Arguments passed. Two+ are expected: a username and the note's message.
     */
    @CommandHandler(value = "note", aliases = { "addnote" }, console = true, remote = true)
    public void onNoteCommand(final CommandSender sender, String[] arguments) {
        if (!sender.hasPermission("command.note")) {
            displayCommandError(sender, "You don't have permission to create a note.");
            return;
        }
        
        if (arguments.length < 2) {
            displayCommandUsage(sender, "/note [username] [message]");
            return;
        }
        
        final String username = arguments[0];
        final String message = StringUtils.join(arguments, " ", 1);
        getAccountManager().findUserId(username).then(new PromiseResultHandler<Integer>() {
            public void onFulfilled(Integer user_id) {
                // TODO: Inform other administrators about this action.
                
                displayCommandSuccess(sender, "The note has been added to **" + username + "**'s profile.");
                PlayerLog.note(user_id, NoteType.INFO, 0, sender.getName(), message).then(new PromiseResultHandler<Integer>() {
                    public void onFulfilled(Integer result) { /** Everything went fine! **/ }
                    public void onRejected(PromiseError error) {
                        getLogger().severe("Unable to add a note to " + username + "'s account: " + error.reason());
                    }
                });
            }
            public void onRejected(PromiseError error) {
                displayCommandError(sender, "Unable to add a note to **" + username + "**'s profile: " + error.reason());
            }
        });
    }
    
    /**
     * Command for displaying the latest notes to a player's account. This implementation is usable
     * from both in-game, the console and from IRC.
     * 
     * @param sender    The player, console or user requesting a user's notes.
     * @param arguments Arguments passed. One is expected: a username.
     */
    @CommandHandler(value = "notes", aliases = { "why" }, console = true, remote = true)
    public void onNotesCommand(final CommandSender sender, String[] arguments) {
        if (!sender.hasPermission("command.notes")) {
            displayCommandError(sender, "You don't have permission to request a player's notes.");
            return;
        }
        
        if (arguments.length == 0) {
            displayCommandUsage(sender, "/notes [username]");
            return;
        }
        
        final String username = arguments[0];
        PlayerLog.findNotes(username).then(new PromiseResultHandler<List<Note>>() {
            public void onFulfilled(List<Note> result) {
                if (result.size() == 0) {
                    displayCommandError(sender, "No notes were found for **" + username + "**.");
                    return;
                }
                
                sender.sendMessage("Displaying " + result.size() + " notes for " + username + ":");
                for (Note note : result)
                    sender.sendMessage("§6  [" + note.date + "] §e" + note.type + "§6 by §e" + note.username + "§6: " + note.message);
            }
            public void onRejected(PromiseError error) {
                displayCommandError(sender, "Unable to fetch the user's notes: " + error.reason());
            }
        });
    }
    
    /**
     * Forcefully disconnects an online player from Mineground. A note will be written to the
     * player's profile containing the reason of the kick.
     * 
     * @param sender    The player, console or user wanting to kick an online player.
     * @param arguments Arguments passed. Two+ are expected: the player's name, and a reason.
     */
    @CommandHandler(value = "kick", console = true, remote = true)
    public void onKickCommand(CommandSender sender, String[] arguments) {
        if (!sender.hasPermission("command.kick")) {
            displayCommandError(sender, "You don't have permission to kick a player from Mineground.");
            return;
        }
        
        if (arguments.length < 2) {
            displayCommandUsage(sender, "/kick [player] [reason]");
            return;
        }
        
        final Player player = getServer().getPlayer(arguments[0]);
        if (player == null) {
            displayCommandError(sender, "The player **" + arguments[0] + "** is not online on Mineground.");
            return;
        }
        
        final String username = player.getName();
        final String reason = StringUtils.join(arguments, " ", 1);
        final Integer userId = getUserId(player);

        // TODO: Inform other administrators about this action.
        
        player.kickPlayer("You have been kicked by " + sender.getName() + " (" + reason + ").");
        
        if (userId == 0)
            return; // we can't write a log message if they weren't logged in to their account.
        
        PlayerLog.note(userId, NoteType.KICK, 0, sender.getName(), reason).then(new PromiseResultHandler<Integer>() {
            public void onFulfilled(Integer result) { /** Everything went fine! **/ }
            public void onRejected(PromiseError error) {
                getLogger().severe("Unable to add a kick note to " + username + "'s account: " + error.reason());
            }
        });
    }
}
