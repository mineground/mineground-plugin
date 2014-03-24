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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.mineground.account.Account;
import com.mineground.account.AccountLevel;
import com.mineground.base.Color;
import com.mineground.base.DisconnectReason;
import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.Message;
import com.mineground.base.TextDecoration;

/**
 * When a player joins or leaves Mineground, we'd like to welcome them and inform the other players
 * about their presence. Furthermore, when a player joins Mineground for the first time, we may want
 * to be a little bit nicer and give them some money and inventory to start with.
 */
public class PlayerSessionMessages extends FeatureBase {
    /**
     * The amount of money a player will receive when they join Mineground for the first time.
     */
    private static final int MONEY_GIFT_ON_FIRST_JOIN = 100;

    /**
     * Message used for welcoming players to Mineground when they join for the first time.
     */
    private final Message mWelcomeFirstTimeMessage;
    
    /**
     * Message to be send to online staff members when a new player joins the server for the first
     * time. This allows them to proactively reach out to that person, in case they need help.
     */
    private final Message mNewPlayerStaffAnnouncement;
    
    /**
     * Announcement which will be distributed when a player joins the server.
     */
    private final Message mPlayerJoinAnnouncement;
    
    /**
     * Announcement which will be distributed when a player has died.
     */
    private final Message mPlayerDeathAnnouncement;
    
    /**
     * Announcement which will be distributed when a player has been killed by another player.
     */
    private final Message mPlayerKilledAnnouncement;
    
    /**
     * Announcement which will be distributed when a player leaves the server.
     */
    private final Message mPlayerQuitAnnouncement;
    
    public PlayerSessionMessages(FeatureInitParams params) {
        super(params);
        
        mWelcomeFirstTimeMessage = Message.Load("first_join");
        mWelcomeFirstTimeMessage.setInteger("money_gift", MONEY_GIFT_ON_FIRST_JOIN);
        
        mNewPlayerStaffAnnouncement = Message.Load("first_join_announcement");
        
        mPlayerJoinAnnouncement = Message.Load("player_join");
        mPlayerDeathAnnouncement = Message.Load("player_death");
        mPlayerKilledAnnouncement = Message.Load("player_killed");
        mPlayerQuitAnnouncement = Message.Load("player_quit");
    }
    
    /**
     * Announces that |player| has joined to server to all online players. If this is the first
     * time |player| joins Mineground, we'll share a brief introduction and inform any online staff
     * of this being the first time the players joins, to proactively offer assistance.
     * 
     * @param player The player who has just joined Mineground.
     */
    public void onPlayerJoined(Player player) {
        // TODO: Don't broadcast join messages if the server just reloaded. We can't use the
        //       Mineground.Lifetime property for this because loading account data is async..
        
        final Account account = getAccountManager().getAccountForPlayer(player);
        final String nickname = player.getName();

        // TODO: Surely there ought to be a better way of removing a single entry from an array?
        final List<Player> onlinePlayers = new ArrayList<Player>();
        for (Player p : getServer().getOnlinePlayers()) {
            //if (player == p)
            //    continue;
            
            onlinePlayers.add(p);
        }
        
        String suffix = "";
        
        // The join message will contain a suffix with the player's level if they're staff. The
        // formatting is done here to ensure having a consistent color. Or maybe my grammar OCD.
        if (AccountLevel.isStaff(account.getLevel())) {
            suffix = Color.GRAY + TextDecoration.ITALIC + " (" +
                    AccountLevel.toString(account.getLevel()).toLowerCase() + ")" +
                    Color.PLAYER_EVENT;
        }

        // Distribute the join announcement for |player| to all other online players.
        mPlayerJoinAnnouncement.setString("nickname", nickname);
        mPlayerJoinAnnouncement.setString("staff_level", suffix);
        mPlayerJoinAnnouncement.send(onlinePlayers, Color.PLAYER_EVENT);
        
        if (account.isFirstJoin()) {
            mNewPlayerStaffAnnouncement.setString("nickname", nickname);
            mNewPlayerStaffAnnouncement.send(getAccountManager().getOnlineStaff(), Color.STAFF_MESSAGE);
            
            // TODO: We need to differentiate between a player's first join ever, and their first
            //       join as a builder. We need to hook in to the registration system for this..

            mWelcomeFirstTimeMessage.setString("nickname", nickname);
            mWelcomeFirstTimeMessage.send(player, Color.YELLOW);
        }
    }
    
    /**
     * Announces that a player has died. The cause of the death could be PvP fighting, common causes
     * such as starvation and falling from a high place, but also just plain stupidity.
     * 
     * @param event The Bukkit PlayerDeathEvent event.
     */
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final EntityDamageEvent damageEvent = player.getLastDamageCause();
        String reason = "";
        
        if (damageEvent != null && (damageEvent instanceof EntityDamageByEntityEvent)) {
            final EntityDamageByEntityEvent entityDamageEvent = (EntityDamageByEntityEvent) damageEvent;
            if (entityDamageEvent.getEntityType() == EntityType.PLAYER) {
                final Player killer = (Player) entityDamageEvent.getEntity();
                
                // TODO: If the weapon used by |killer| to kill |player| has a name, it should be
                //       included in the message as |reason|.
                
                mPlayerKilledAnnouncement.setString("nickname", player.getName());
                mPlayerKilledAnnouncement.setString("killer", killer.getName());
                mPlayerKilledAnnouncement.setString("reason", reason);
                mPlayerKilledAnnouncement.send(getServer().getOnlinePlayers(), Color.PLAYER_EVENT);
                return;
            }
        }
        
        if (damageEvent != null)
            reason = explanationForDamageCause(damageEvent);
        
        mPlayerDeathAnnouncement.setString("nickname", player.getName());
        mPlayerDeathAnnouncement.setString("reason", reason);
        mPlayerDeathAnnouncement.send(getServer().getOnlinePlayers(), Color.PLAYER_EVENT);
    }
    
    /**
     * Announces that |player| is leaving the server to all online players.
     * 
     * @param player The player who is leaving the server.
     * @param reason The reason why the player disconnected from Mineground.
     */
    public void onPlayerDisconnect(Player player, DisconnectReason reason) {
        if (reason == DisconnectReason.SHUTDOWN)
            return;

        mPlayerQuitAnnouncement.setString("nickname", player.getName());
        mPlayerQuitAnnouncement.send(getServer().getOnlinePlayers(), Color.PLAYER_EVENT);
    }
    
    /**
     * Translates entries in the DamageCause enumeration to a textual representation. If the cause
     * cannot be exactly determined, an empty string will be returned instead.
     * 
     * @param damageCause   The damage cause to get a textual representation for.
     * @return              Textual representation of |damageCause|.
     */
    private String explanationForDamageCause(EntityDamageEvent damageEvent) {
        // TODO: Determine the exact reason of their death.
        return "";
    }
}
