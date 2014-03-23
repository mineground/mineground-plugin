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

package com.mineground.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Whereas the Mineground configuration is not mutable from within the plugin itself, features will
 * need a way for storing persistent settings as well. The settings class is another YAML file, but
 * made mutable from within the plugin.
 */
public class Settings extends YamlConfiguration {
    /**
     * The file in which all settings should be stored. The <code>save()</code> method will save
     * the class' current state to that file.
     */
    private final File mSettingsFile;
    
    public Settings(File settingsFile) {
        super();

        mSettingsFile = settingsFile;
        try {
            load(settingsFile);
        } catch (FileNotFoundException exception) {
            // Ignore this exception, since this will not happen in practice.
        } catch (IOException exception) {
            // I/O error means that the file is not readable. Throw an error.
            Logger.getLogger(getClass().getCanonicalName()).severe("ERROR: Cannot read from settings.yml");
            exception.printStackTrace();
            
        } catch (InvalidConfigurationException exception) {
            // This means that the settings file somehow got corrupted. Great.
            Logger.getLogger(getClass().getCanonicalName()).severe("ERROR: settings.yml contains invalid content.");
            exception.printStackTrace();
        }
    }
    
    /**
     * Saves the current state of Mineground's settings to the settings file.
     */
    public void save() {
        try {
            save(mSettingsFile);
        } catch (IOException exception) {
            // This means that the settings couldn't be written to. Great.
            Logger.getLogger(getClass().getCanonicalName()).severe("ERROR: settings.yml could not be written to.");
            exception.printStackTrace();
        }
    }
    
}
