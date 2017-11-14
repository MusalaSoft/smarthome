/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.bluetooth.bluez.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.binding.bluetooth.BluetoothAdapter;
import org.eclipse.smarthome.binding.bluetooth.BluetoothAddress;
import org.eclipse.smarthome.binding.bluetooth.BluetoothBindingConstants;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDiscoveryListener;
import org.eclipse.smarthome.binding.bluetooth.bluez.BlueZAdapterConstants;
import org.eclipse.smarthome.binding.bluetooth.bluez.BluezBluetoothDevice;
import org.eclipse.smarthome.binding.bluetooth.bluez.internal.dbus.Adapter1;
import org.eclipse.smarthome.binding.bluetooth.bluez.internal.dbus.ObjectManager;
import org.eclipse.smarthome.binding.bluetooth.bluez.internal.dbus.ObjectManager.InterfacesAdded;
import org.eclipse.smarthome.binding.bluetooth.bluez.internal.dbus.ObjectManager.InterfacesRemoved;
import org.eclipse.smarthome.binding.bluetooth.bluez.internal.dbus.Properties.PropertiesChanged;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BlueZBridgeHandler} is responsible for interfacing to the BlueGiga Bluetooth adapter.
 * It provides a private interface for {@link BlueZBluetoothDevice}s to access the dongle and provides top
 * level adaptor functionality for scanning and arbitration.
 * <p>
 * The handler provides the serial interface to the dongle via the BlueGiga BG-API library.
 * <p>
 * In the BlueGiga dongle, we leave scanning enabled most of the time. Normally, it's just passive scanning, and active
 * scanning is enabled when we want to include new devices. Passive scanning is enough for us to receive beacons etc
 * that are transmitted periodically, and active scanning will get more information which may be useful when we are
 * including new devices.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - DBUS/BlueZ specific code from initial prototype
 */
@SuppressWarnings("rawtypes")
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.ARRAY_CONTENTS,
        DefaultLocation.TYPE_BOUND, DefaultLocation.TYPE_PARAMETER })
public class BlueZBridgeHandler extends BaseBridgeHandler implements BluetoothAdapter, DBusSigHandler {

    private static final Logger logger = LoggerFactory.getLogger(BlueZBridgeHandler.class);

    public static final int STATE_OFF = 10;
    public static final int STATE_ON = 12;

    private DBusConnection connection;
    private DBus.Properties propertyReader;
    private String dbusPath;
    private Adapter1 adapter1;

    // Our BT address
    private BluetoothAddress address;

    // Map of Bluetooth devices known to this bridge.
    // This is all devices we have heard on the network - not just things bound to the bridge
    private final Map<BluetoothAddress, BluetoothDevice> devices = new ConcurrentHashMap<>();

    // Set of discovery listeners
    protected final Set<BluetoothDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();

    private Object name;

    private boolean scanning = false;

    private int state;

    private boolean leReady;

    public BlueZBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        dbusPath = BlueZAdapterConstants.BLUEZ_DBUS_PATH + "/" + getThing().getUID().getId();
        logger.debug("Creating BlueZ adapter at '{}'", dbusPath);

        try {
            try {
                String dbusAddress = System.getProperty(BlueZAdapterConstants.BLUEZ_DBUS_CONFIGURATION);
                if (dbusAddress == null) {
                    connection = DBusConnection.getConnection(DBusConnection.SYSTEM);
                } else {
                    connection = DBusConnection.getConnection(dbusAddress);
                }
                logger.debug("BlueZ connection opened at {}", connection.getUniqueName());
            } catch (DBusExecutionException e) {
                logger.error("DBus method failed connecting: {}", e.getMessage());
            }

            try {
                ObjectManager objectManager = connection.getRemoteObject(BlueZAdapterConstants.BLUEZ_DBUS_SERVICE, "/",
                        ObjectManager.class);
                Map<Path, Map<String, Map<String, Variant>>> managedObjects = objectManager.GetManagedObjects();

                // Notify our user(s) of any devices that already exist - otherwise they won't know about them!
                if (managedObjects != null) {
                    for (Map<String, Map<String, Variant>> managedObject : managedObjects.values()) {
                        Map<String, Variant> deviceProperties = managedObject
                                .get(BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_DEVICE1);
                        if (deviceProperties == null) {
                            continue;
                        }

                        Variant adapterPath = deviceProperties
                                .get(BlueZAdapterConstants.BLUEZ_DBUS_DEVICE_PROPERTY_ADAPTER);
                        if (adapterPath == null) {
                            continue;
                        }

                        String adapterName = adapterPath.getValue().toString();
                        if (dbusPath.equals(adapterName)) {
                            addInterface(deviceProperties);
                        }
                    }
                }
            } catch (DBusExecutionException e) {
                logger.error("DBus method failed getting managed objects: {}", e.getMessage());
            }

            try {
                propertyReader = connection.getRemoteObject(BlueZAdapterConstants.BLUEZ_DBUS_SERVICE, dbusPath,
                        DBus.Properties.class);

                Map<String, Variant> properties = propertyReader
                        .GetAll(BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_ADAPTER1);
                updateProps(properties);

                connection.addSigHandler(PropertiesChanged.class, this);
                connection.addSigHandler(InterfacesAdded.class, this);
                connection.addSigHandler(InterfacesRemoved.class, this);

                adapter1 = connection.getRemoteObject(BlueZAdapterConstants.BLUEZ_DBUS_SERVICE, dbusPath,
                        Adapter1.class);

                // Setting the discovery filter should ensure we get more notifications about RSSI
                Map<String, Variant> scanProperties = new HashMap<>(2);
                scanProperties.put(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_FILTER_RSSI,
                        new Variant(new Short((short) -125)));
                scanProperties.put(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_FILTER_TRANSPORT,
                        new Variant(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_TRANSPORT_LE));
                adapter1.SetDiscoveryFilter(scanProperties);
            } catch (DBusExecutionException e) {
                logger.error("DBus method failed setting handlers: {}", e.getMessage());
            }
        } catch (DBusException e) {
            e.printStackTrace();
        }
    }

    public String getDbusPath() {
        return dbusPath;
    }

    @Override
    public ThingUID getUID() {
        return getThing().getUID();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addDiscoveryListener(BluetoothDiscoveryListener listener) {
        discoveryListeners.add(listener);

    }

    @Override
    public void removeDiscoveryListener(@Nullable BluetoothDiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    @Override
    public void scanStart() {
        for (BluetoothDevice device : devices.values()) {
            notifyEventListeners(device);
        }

        // Now start the discovery - only if we're not already discovering though!
        if (isDiscovering() == false) {
            adapter1.StartDiscovery();
        }
    }

    @Override
    public void scanStop() {
        // Only stop discovery if there is a discovery in progress
        // Otherwise this can cause an exception
        if (isDiscovering() == false) {
            return;
        }

        try {
            adapter1.StopDiscovery();
        } catch (DBusExecutionException e) {
            logger.debug("Stopping discovery has failed: {}", e.getMessage());
        }
    }

    @Override
    public BluetoothAddress getAddress() {
        return address;
    }

    @Override
    public BluetoothDevice getDevice(BluetoothAddress address) {
        if (devices.containsKey(address)) {
            return devices.get(address);
        }
        return new BluezBluetoothDevice(this, address, "");
    }

    @Override
    public void dispose() {
        finalize();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void finalize() {
        try {
            if (connection != null) {
                connection.removeSigHandler(PropertiesChanged.class, this);
                connection.removeSigHandler(InterfacesAdded.class, this);
                connection.removeSigHandler(InterfacesRemoved.class, this);

                connection.disconnect();
            }
        } catch (DBusException e) {
            logger.error("DBus failed on finalisation: {}", e.getMessage(), e);
        }
    }

    protected int getState() {
        return state;
    }

    protected boolean isEnabled() {
        return getState() == STATE_ON ? true : false;
    }

    protected boolean isDiscovering() {
        return scanning;
    }

    protected void enable() {
        if (propertyReader == null) {
            return;
        }

        propertyReader.Set(BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_ADAPTER1,
                BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_POWERED, true);
    }

    protected void disable() {
        if (propertyReader == null) {
            return;
        }

        propertyReader.Set(BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_ADAPTER1,
                BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_POWERED, false);
    }

    /**
     *
     * @param properties
     */
    private void addInterface(Map<String, Variant> properties) {
        for (Entry<String, Variant> entry : properties.entrySet()) {
            logger.trace("Property: {} is {}", entry.getKey(), entry.getValue());
        }

        String newAddress = properties.get(BlueZAdapterConstants.BLUEZ_DBUS_DEVICE_PROPERTY_ADDRESS).getValue()
                .toString();
        if (newAddress == null) {
            logger.debug("Address not known. Aborting addDevice.");
            return;
        }

        BluetoothDevice bluetoothDevice;
        // Make sure we don't already know about this device
        BluetoothAddress address = new BluetoothAddress(newAddress);
        if (devices.containsKey(address)) {
            bluetoothDevice = devices.get(address);
        } else {
            bluetoothDevice = new BluezBluetoothDevice(this, properties);
            devices.put(bluetoothDevice.getAddress(), bluetoothDevice);
            logger.debug("Device '{}' added Ok.", bluetoothDevice.getName());
        }

        // Send the notification even if we know about this device already.
        notifyEventListeners(bluetoothDevice);
    }

    /**
     *
     * @param device
     */
    private void removeDevice(InterfacesRemoved device) {
        logger.info("BlueZ removeDevice: {}", device.object_path);
        // TODO: Do something!
    }

    @Override
    public void handle(@Nullable DBusSignal signal) {
        try {
            if (signal.getName().equals(BlueZAdapterConstants.BLUEZ_DBUS_SIGNAL_PROPERTIESCHANGED)) {
                // Make sure it's for us
                if (dbusPath.equals(signal.getPath()) == false) {
                    return;
                }

                PropertiesChanged propertiesChanged = (PropertiesChanged) signal;

                if (BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_ADAPTER1
                        .equals(propertiesChanged.interface_name) == false) {
                    return;
                }
                if (propertiesChanged.changed_properties.size() != 0) {
                    logger.debug("{}: Properties changed: {}", dbusPath, propertiesChanged.changed_properties);
                    updateProps(propertiesChanged.changed_properties);
                }
                if (propertiesChanged.invalidated_properties.size() != 0) {
                    // TODO: Implement this
                    logger.debug("{}: Properties invalid: {}", dbusPath, propertiesChanged.invalidated_properties);
                }
            } else if (signal.getName().equals(BlueZAdapterConstants.BLUEZ_DBUS_SIGNAL_INTERFACESADDED)) {
                // Get the properties for this device
                // If this is not a BlueZ device, then this will return null
                Map<String, Variant> properties = ((InterfacesAdded) signal).interfaces_and_properties
                        .get(BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_DEVICE1);
                if (properties == null) {
                    return;
                }

                addInterface(properties);
            } else if (signal instanceof InterfacesRemoved) {
                removeDevice((InterfacesRemoved) signal);
            } else {
                logger.info("Unknown signal!!! {}", signal.getClass());
            }
        } catch (DBusExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the adapter configuration from the Bluez DBus properties
     *
     * @param changed_properties
     */
    @SuppressWarnings("unchecked")
    private void updateProps(Map<String, Variant> properties) {
        for (String property : properties.keySet()) {
            logger.trace("Adapter '{}' updated property: {} to {}", dbusPath, property,
                    properties.get(property).getValue());
            switch (property) {
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_NAME:
                    // Name can't change, so if it's already set, then ignore
                    // This allows the name to be used if the alias isn't set
                    if (name == null || name == "") {
                        name = properties.get(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_NAME).getValue();
                    }
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_ALIAS:
                    name = properties.get(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_ALIAS).getValue();
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_ADDRESS:
                    address = new BluetoothAddress((String) properties
                            .get(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_ADDRESS).getValue());
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_DISCOVERING:
                    scanning = (Boolean) properties.get(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_DISCOVERING)
                            .getValue();
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_POWERED:
                    state = ((Boolean) properties.get(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_POWERED)
                            .getValue()) ? STATE_ON : STATE_OFF;
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_UUIDS:
                    // Check if we support the GATT profile. If so, then we're LE ready
                    leReady = false;
                    Vector<String> uuids = ((Vector<String>) properties
                            .get(BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_UUIDS).getValue());
                    for (String uuid : uuids) {
                        if (BluetoothBindingConstants.PROFILE_GATT.toString().equals(uuid)) {
                            leReady = true;
                            break;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void invalidateDeviceProperties(List<String> invalidated_properties) {
        for (String property : invalidated_properties) {
            logger.trace("GATT Service '{}' invalidated property: {}", dbusPath, property);
            switch (property) {
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_NAME:
                    // Name can't change, so if it's already set, then ignore
                    // This allows the name to be used if the alias isn't set
                    name = "";
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_ALIAS:
                    name = "";
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_DISCOVERING:
                    scanning = false;
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_POWERED:
                    state = STATE_OFF;
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_ADAPTER_PROPERTY_UUIDS:
                    // Check if we support the GATT profile. If so, then we're LE ready
                    leReady = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void notifyEventListeners(BluetoothDevice device) {
        for (BluetoothDiscoveryListener listener : discoveryListeners) {
            listener.deviceDiscovered(device);
        }
    }

}
