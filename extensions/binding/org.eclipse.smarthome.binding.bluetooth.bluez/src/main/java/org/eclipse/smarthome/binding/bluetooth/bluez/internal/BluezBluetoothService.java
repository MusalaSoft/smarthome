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

import org.eclipse.smarthome.binding.bluetooth.BluetoothService;
import org.eclipse.smarthome.binding.bluetooth.bluez.BlueZAdapterConstants;
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
 * Implementation of a BluetoothService for BlueZ
 *
 * @author Chris Jackson - Initial Contribution
 * @author Kai Kreuzer - Refactored code to latest APIs
 *
 */
@SuppressWarnings("rawtypes")
public class BluezBluetoothService extends BluetoothService implements DBusSigHandler {
    private final Logger logger = LoggerFactory.getLogger(BluezBluetoothService.class);

    private DBusConnection connection;
    private final String dbusPath;

    @SuppressWarnings("unchecked")
    public BluezBluetoothService(UUID uuid) {
        super(uuid);

        logger.debug("Creating BlueZ GATT service at '{}'", uuid);

        dbusPath = uuid.toString();

        try {
            String dbusAddress = System.getProperty(BlueZAdapterConstants.BLUEZ_DBUS_CONFIGURATION);
            if (dbusAddress == null) {
                connection = DBusConnection.getConnection(DBusConnection.SYSTEM);
            } else {
                connection = DBusConnection.getConnection(dbusAddress);
            }
            // logger.debug("BlueZ connection opened at {}", connection.getUniqueName());

            DBus.Properties propertyReader = connection.getRemoteObject(BlueZAdapterConstants.BLUEZ_DBUS_SERVICE,
                    dbusPath, DBus.Properties.class);
            Map<String, Variant> properties = propertyReader
                    .GetAll(BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_GATTSERVICE1);
            updateProperties(properties);

            connection.addSigHandler(PropertiesChanged.class, this);
        } catch (DBusException e) {
            logger.debug("Error initialising GATT service {}: {}", dbusPath, e.getMessage(), e);
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
            logger.error("Error finalising Bluetooth service {}: {}", dbusPath, e.getMessage(), e);
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

                if (BlueZAdapterConstants.BLUEZ_DBUS_INTERFACE_GATTSERVICE1
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
            logger.trace("GATT Service '{}' updated property: {} to {}", dbusPath, property,
                    properties.get(property).getValue());
            switch (property) {
                case BlueZAdapterConstants.BLUEZ_DBUS_GATTSERVICE_PROPERTY_PRIMARY:
                    primaryService = (Boolean) properties
                            .get(BlueZAdapterConstants.BLUEZ_DBUS_GATTSERVICE_PROPERTY_PRIMARY).getValue();
                    break;
                case BlueZAdapterConstants.BLUEZ_DBUS_GATTSERVICE_PROPERTY_CHARACTERISTICS:
                    supportedCharacteristics.clear();
                    Vector<Object> newCharacteristics = (Vector<Object>) properties
                            .get(BlueZAdapterConstants.BLUEZ_DBUS_GATTSERVICE_PROPERTY_CHARACTERISTICS).getValue();
                    if (newCharacteristics != null && newCharacteristics.isEmpty() == false) {
                        for (Object characteristic : newCharacteristics) {
                            // TODO: How to get hold of the correct handle? (currently we set 0)
                            addCharacteristic(
                                    new BluezBluetoothCharacteristic(UUID.fromString(characteristic.toString()), 0));
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    }
}
