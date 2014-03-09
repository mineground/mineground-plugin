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

    public Account() {
        mAccountLevel = AccountLevel.Guest;
    }
    
    /**
     * Initializes this account based on the information available in |accountData|.
     * 
     * @param accountData The account data to be associated with this account.
     */
    public void initialize(AccountData accountData) {
        mAccountLevel = accountData.level;
        mAccountData = accountData;
    }
    
    /**
     * Returns whether this account is a guest account.
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
