/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.bluetooth.bluez.internal;

import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import org.eclipse.smarthome.binding.bluetooth.BluetoothCharacteristic;
import org.eclipse.smarthome.binding.bluetooth.bluez.BlueZAdapterConstants;
import org.eclipse.smarthome.binding.bluetooth.bluez.internal.dbus.GattCharacteristic1;
import org.eclipse.smarthome.binding.bluetooth.bluez.internal.dbus.Properties.PropertiesChanged;
import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BluetoothCharacteristic for BlueZ
 *
 * @author Chris Jackson - Initial Contribution
 *
 */
@SuppressWarnings("rawtypes")
public class BluezBluetoothCharacteristic extends BluetoothCharacteristic implements DBusSigHandler {
    private static final Logger logger = LoggerFactory.getLogger(BluezBluetoothCharacteristic.class);

    private DBusConnection connection;
    private final String dbusPath;

    private GattCharacteristic1 characteristic1;

    @SuppressWarnings("unchecked")
    public BluezBluetoothCharacteristic(UUID uuid, int handle) {
        super(uuid, handle);

        dbusPath = uuid.toString();

        try {
            String dbusAddress = System.getProperty(BlueZAdapterConstants.BLUEZ_DBUS_CONFIGURATION);
            if (dbusAddress == null) {
                connection = DBusConnection.getConnection(DBusConnection.SYSTEM);
            } else {
                connection = DBusConnection.getConnection(dbusAddress);
            }

            DBus.Properties propertyReader = connection.getRemoteObject(BlueZAdapterConstants.BLUEZ_DBUS_SERVICE,
                    dbusPath, DBus.Properties.class);
            Map<String, Variant> properties = propertyReader
                    .GetAll(BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_GATTCHARACTERISTIC1);
            updateProperties(properties);

            connection.addSigHandler(PropertiesChanged.class, this);

            characteristic1 = connection.getRemoteObject(BlueZAdapterConstants.BLUEZ_DBUS_SERVICE, dbusPath,
                    GattCharacteristic1.class);
        } catch (DBusException e) {
            logger.debug("Error initialising Bluetooth characteristic {}: {}", dbusPath, e.getMessage(), e);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public void finalize() {
        try {
            if (connection != null) {
                connection.removeSigHandler(PropertiesChanged.class, this);
                connection.disconnect();
            }
        } catch (DBusException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void handle(DBusSignal signal) {
        try {
            if (signal instanceof PropertiesChanged) {
                // Make sure it's for us
                if (dbusPath.equals(signal.getPath()) == false) {
                    return;
                }

                PropertiesChanged propertiesChanged = (PropertiesChanged) signal;

                if (BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_GATTCHARACTERISTIC1
                        .equals(propertiesChanged.interface_name) == false) {
                    return;
                }
                if (propertiesChanged.changed_properties.size() != 0) {
                    logger.debug("{}: Properties changed: {}", dbusPath, propertiesChanged.changed_properties);
                    updateProperties(propertiesChanged.changed_properties);
                }
                if (propertiesChanged.invalidated_properties.size() != 0) {
                    logger.debug("{}: Properties invalid: {}", dbusPath, propertiesChanged.invalidated_properties);
                }
            }
        } catch (DBusExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Updates the gatt service configuration from the Bluez DBus properties
     *
     * @param changed_properties
     */
    @SuppressWarnings("unchecked")
    private void updateProperties(Map<String, Variant> properties) {
        for (String property : properties.keySet()) {
            logger.debug("GATT Characteristic '{}' updated property: {} to {}", dbusPath, property,
                    properties.get(property).getValue());
            switch (property) {
                case BlueZAdapterConstants.BLUEZ_DBUS_GATTCHARACTERISTIC_PROPERTY_UUID:
                    uuid = UUID.fromString((String) properties
                            .get(BlueZAdapterConstants.BLUEZ_DBUS_GATTCHARACTERISTIC_PROPERTY_UUID).getValue());
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_GATTCHARACTERISTIC_PROPERTY_VALUE:
                    setValue((int[]) properties.get(BlueZAdapterConstants.BLUEZ_DBUS_GATTCHARACTERISTIC_PROPERTY_VALUE)
                            .getValue());
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_GATTCHARACTERISTIC_PROPERTY_DESCRIPTORS:
                    gattDescriptors.clear();
                    Vector<Object> newDescriptors = (Vector<Object>) properties
                            .get(BlueZAdapterConstants.BLUEZ_DBUS_GATTCHARACTERISTIC_PROPERTY_DESCRIPTORS).getValue();
                    if (newDescriptors != null && newDescriptors.isEmpty() == false) {
                        logger.debug("Characteristics returned {}", newDescriptors);
                    }
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_GATTCHARACTERISTIC_PROPERTY_FLAGS:
                    Vector<Object> flags = (Vector<Object>) properties
                            .get(BlueZAdapterConstants.BLUEZ_DBUS_GATTCHARACTERISTIC_PROPERTY_FLAGS).getValue();
                    if (flags.contains("read")) {
                        this.properties += PROPERTY_READ;
                    }
                    if (flags.contains("write")) {
                        this.properties += PROPERTY_WRITE;
                    }
                    if (flags.contains("notify")) {
                        this.properties += PROPERTY_NOTIFY;
                    }
                    // TODO: Other properties?
                    break;

                default:
                    break;
            }
        }
    }

}
