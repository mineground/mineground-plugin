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

package com.mineground;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.entity.Player;

// The EventDispatcher listens to all incoming events from Bukkit, validates them, and invokes all
// observers within the Mineground plugin which depend on them.
public class EventDispatcher {
    private enum EventTypes {
        // Invoked when the Mineground plugin gets loaded by the Bukkit server.
        MinegroundLoadedEvent("onMinegroundLoaded"),
        
        // Invoked when the Mineground plugin gets unloaded by the Bukkit server.
        MinegroundUnloadEvent("onMinegroundUnloaded"),
        
        // Invoked when a player has joined Mineground, and the handshake with the Minecraft
        // server has successfully commenced. Their account is available at this point.
        PlayerJoinedEvent("onPlayerJoined"),
        
        // Invoked when a player disconnects from Mineground. Their account information is still
        // mutable during this call, but it will be serialized immediately after.
        PlayerQuitEvent("onPlayerQuit");
        
        // -----------------------------------------------------------------------------------------
        
        private String name;
        private EventTypes(String eventName) {
            name = eventName;
        }
    }
    
    // Information regarding an individual event observer: |method| on the |object| instance.
    private class EventObserver {
        private WeakReference<Object> instance;
        private Method method;
        
        private EventObserver(Object instance_, Method method_) {
            instance = new WeakReference<Object>(instance_);
            method = method_;
        }
    }
    
    // Hold weak references to the Feature instances, given that we have no interest in keeping them
    // alive. If a reference has been invalidated, the Feature should be removed from all observers.
    final private Map<EventTypes, ArrayList<EventObserver>> mObserverListMap;
    
    // Maintain a map between event method name -> event type, allowing more efficient 
    final private Map<String, EventTypes> mEventNameToTypeMap;
    
    public EventDispatcher() {
        // An EnumMap is usually represented as a plain array in Java implementations, meaning
        // all operations should be constant time (but this is not guaranteed). Initialize both the
        // map and lists for all possible events here, so we don't have to do null-checks elsewhere.
        mObserverListMap = new EnumMap<EventTypes, ArrayList<EventObserver>>(EventTypes.class);
        mEventNameToTypeMap = new HashMap<String, EventTypes>();

        for (EventTypes eventType : EventTypes.values()) {
            mObserverListMap.put(eventType, new ArrayList<EventObserver>());
            mEventNameToTypeMap.put(eventType.name, eventType);
        }
    }
    
    // Registers all event listeners in |instance| with the observer lists owned by this dispatcher.
    // Reflection is used to find the relevant method names (as dictated by the EventTypes enum
    // defined earlier in this class) on the instance.
    public void registerListeners(Object instance) {
        Method[] reflectionMethods = instance.getClass().getMethods();
        for (Method reflectionMethod : reflectionMethods) {
            EventTypes eventType = mEventNameToTypeMap.get(reflectionMethod.getName());
            if (eventType == null)
                continue;

            mObserverListMap.get(eventType).add(new EventObserver(instance, reflectionMethod));
        }
    }
    
    // Dispatches an event of type |event| to all attached observers, optionally passing |arguments|
    // as the arguments to the to be invoked observer.
    private void dispatch(EventTypes event, Object... arguments) {
        final Iterator<EventObserver> observers = mObserverListMap.get(event).iterator();
        while (observers.hasNext()) {
            final EventObserver observer = observers.next();
            final Object instance = observer.instance.get();

            // If |instance| is null, the object instance itself has lost all its references
            // elsewhere in the plugin, and we thus can't deliver events to it anymore.
            if (instance == null) {
                observers.remove();
                continue;
            }

            try {
                observer.method.invoke(instance, arguments);
            } catch (IllegalArgumentException e) {
                // TODO: Log an error because the method has an invalid signature.
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // Ignored, we do a null check on |feature| earlier on in this method. By getting
                // the weak pointer's value we create a strong reference too, stopping the instance
                // from being garbage collected while we're dispatching an event on it.
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO: Log an error because the method is not accessible.
                e.printStackTrace();
            }
        }
    }
    
    public void onMinegroundLoaded()   { dispatch(EventTypes.MinegroundLoadedEvent); }
    public void onMinegroundUnloaded() { dispatch(EventTypes.MinegroundUnloadEvent); }
    
    public void onPlayerJoined(Player player) { dispatch(EventTypes.PlayerJoinedEvent, player); }
    public void onPlayerQuit(Player player) { dispatch(EventTypes.PlayerQuitEvent, player); }
}
