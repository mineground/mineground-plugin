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

package com.mineground.account;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

/**
 * The Account class contains all information, settings and options in regards to an individual
 * player. The instances themselves should be retrieved through the AccountManager. All changes made
 * to this object, including its members, will persist between playing sessions.
 */
public class Account {
    /**
     * Data, originating from the database, associated with this account. This value may be NULL
     * when Mineground is experiencing database issues.
     */
    private AccountData mAccountData;
    
    /**
     * Level of this player on Mineground. This greatly influences the available commands and
     * abilities of the player. Management members will be made server operators automatically.
     */
    private AccountLevel mAccountLevel;
    
    /**
     * The Permission Attachment for this player, which belongs to the player but is owned by the
     * Mineground plugin. This allows us to grant them additional permissions.
     */
    private PermissionAttachment mPermissionAttachment;

    public Account() {
        mAccountLevel = AccountLevel.Guest;
    }
    
    /**
     * Returns whether the owner of this account has authenticated with it.
     * 
     * @return True if the account is authenticated.
     */
    public boolean isAuthenticated() {
        return mAccountData != null;
    }
    
    /**
     * Initializes this account based on the information available in |accountData|. Mineground will
     * automatically grant the "mineground.[level]" permission to this player, which basically is
     * a list of the permissions available for that level. Management members will automatically
     * receive server operator rights as well.
     * 
     * @param accountData           The account data to be associated with this account.
     * @param player                The player who owns this account, for applying permissions.
     * @param permissionAttachment  The Mineground-owned permission attachment for this player.
     */
    public void initialize(AccountData accountData, Player player, PermissionAttachment permissionAttachment) {
        mAccountData = accountData;
        mAccountLevel = accountData.level;
        mPermissionAttachment = permissionAttachment;
        
        final String normalizedLevel = AccountLevel.toString(mAccountLevel).toLowerCase();

        permissionAttachment.setPermission("mineground." + normalizedLevel, true);
        if (mAccountLevel == AccountLevel.Management)
            player.setOp(true);
    }
    
    /**
     * Terminates the account settings of |player|. Revoke all granted permissions by removing our
     * permission attachment from the player.
     * 
     * @param player The player to terminate account settings of.
     */
    public void terminate(Player player) {
        if (mAccountData == null)
            return;
        
        player.removeAttachment(mPermissionAttachment);
        if (mAccountLevel == AccountLevel.Management)
            player.setOp(false);
        
        mAccountData = null;
    }
    
    /**
     * Returns the user Id which identifies this Id in the database. Many other tables reference to
     * this Id to indicate which player they mean.
     * 
     * @return The account's user Id.
     */
    public int getUserId() {
        if (mAccountData == null)
            return 0;
        
        return mAccountData.user_id;
    }
    
    /**
     * Returns the chat prefix of this player. Players have the ability to choose a prefix of choice
     * based on their level, group and whether they've donated or not, which can be changed using
     * the "/my prefix" command.
     * 
     * @return  The prefix this player wishes to use when chatting.
     */
    public String getChatPrefix() {
        // TODO: This should be settable rather than be based on the player's level. Fix this when
        //       we introduce the "/my prefix" command.
        switch (mAccountLevel) {
            case Guest:
                return "[Guest] ";
            case Builder:
                return "[§1Builder§f] ";
            case SBuilder:
                return "[§9SBuilder§f] ";
            case VIP:
                return "[§2VIP§f] ";
            case Moderator:
                return "[§cMod§f] ";
            case Administrator:
                return "[§cAdmin§f] ";
            case Management:
                return "[§cAdmin§f] ";
        }
        
        return "[Unknown] ";
    }
    
    /**
     * Returns whether this player has enabled PVP for their account. This means that when they're
     * in worlds which allow PVP, other players can attack them.
     * 
     * @return  Whether this player allows PVP.
     */
    public boolean getPvp() {
        // TODO: Implement getPvp();
        return true;
    }
    
    /**
     * Sets whether this player enabled PVP. When disabled, other players will not be able to do
     * harm to them anymore.
     * 
     * @param enabled   Whether PVP should be enabled.
     */
    public void setPvp(boolean enabled) {
        // TODO: Implement setPvp();
    }
    
    /**
     * Returns the location Id of this account's home. Players have the ability to warp to this
     * using the /home command, or update its location using the "/home set" command.
     * 
     * @return Location Id of their home.
     */
    public int getHomeLocation() {
        if (mAccountData == null)
            return 0;
        
        return mAccountData.home_location;
    }
    
    /**
     * Updates this account's home location Id to |locationId|. The /home command will now teleport
     * them to the location stored using that Id instead.
     * 
     * @param locationId New Id of the location containing their home.
     */
    public void setHomeLocation(int locationId) {
        if (mAccountData != null)
            mAccountData.home_location = locationId;
    }
    
    /**
     * Returns whether this account is a guest account.
     *
     * @return Whether this account represents a guest.
     */
    public boolean isGuest() {
        return mAccountLevel == AccountLevel.Guest || mAccountData == null;
    }
    
    /**
     * Returns whether this is the first time this account is seen on Mineground. While the player
     * most likely is still a Guest at this point, features may want to be slightly nicer.
     *
     * @return Whether this is the first time this player joined Mineground.
     */
    public boolean isFirstJoin() {
        return false;
    }
    
    /**
     * Returns the level of this account.
     *
     * @return The level of the player owning this account.
     */
    public AccountLevel getLevel() {
        return mAccountLevel;
    }
    
    /**
     * Returns the AccountData instance associated with this account. This could be NULL when the
     * information could not be loaded earlier on.
     *
     * @return Account data associated with this account.
     */
    public AccountData getAccountData() {
        return mAccountData;
    }
}
