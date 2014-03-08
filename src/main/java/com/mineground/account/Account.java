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

import java.util.logging.Logger;

// The Account class contains all information, settings and options in regards to an individual
// player. The instances themselves should be retrieved through the AccountManager. All changes made
// to this object, including its members, will persist between playing sessions.
public class Account {
    // 
    private AccountData mAccountData;

    public void load(AccountData accountData) {
        Logger.getLogger("account").info(accountData.username + " (Id: " + accountData.user_id + ") has been online for " +
                accountData.online_time + " seconds!");
        
        mAccountData = accountData;
    }
    
    public boolean isGuest() {
        return false;
    }
    
    public boolean isFirstJoin() {
        return false;
    }
    
    public AccountData getAccountData() {
        return mAccountData;
    }
}
