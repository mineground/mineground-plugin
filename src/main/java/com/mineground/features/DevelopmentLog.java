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

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mineground.base.FeatureBase;
import com.mineground.base.FeatureCommand;
import com.mineground.base.FeatureInitParams;
import com.mineground.base.PromiseError;
import com.mineground.base.PromiseResultHandler;
import com.mineground.database.DatabaseResult;

public class DevelopmentLog extends FeatureBase {
    public DevelopmentLog(FeatureInitParams params) {
        super(params);
    }
    
    public void onMinegroundLoaded() { }
    public void onMinegroundUnloaded() { }
    
    public void onPlayerJoined(Player player) {
        player.sendMessage("Welcome on Mineground, " + player.getName());
        
        // TODO: Convert this example to use DatabaseStatement.
        final String playerRequest = "SELECT * FROM users WHERE user_uuid = \"" + player.getUniqueId().toString() + "\"";
        getDatabase().query(playerRequest).then(new PromiseResultHandler<DatabaseResult>() {
            public void onFulfilled(DatabaseResult result) {
                // TODO: Validate the contents of |result| in here.
            }
            public void onRejected(PromiseError error) {
                getLogger().warning("Database error: " + error.reason());
            }
        });
    }
    
    @FeatureCommand(value = "bread", description = "Gives you some bread to eat.", console = false)
    public boolean onBreadCommand(CommandSender sender, Command command, String[] arguments) {
        Player player = (Player) sender;
        player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
        
        sender.sendMessage("Enjoy your bread!");
        return true;
    }
}
