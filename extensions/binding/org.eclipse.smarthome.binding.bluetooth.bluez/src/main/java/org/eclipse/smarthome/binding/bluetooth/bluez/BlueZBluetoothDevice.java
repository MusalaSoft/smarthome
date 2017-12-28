/**
 * Copyright (c) 2014,2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.binding.bluetooth.bluez;

import org.eclipse.smarthome.binding.bluetooth.BluetoothAddress;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice;
import org.eclipse.smarthome.binding.bluetooth.bluez.handler.BlueZBridgeHandler;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothConnectionStatusNotification;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothScanNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BluetoothDevice for BlueZ via TinyB
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class BlueZBluetoothDevice extends BluetoothDevice {

    private tinyb.BluetoothDevice device;

    private static final Logger logger = LoggerFactory.getLogger(BlueZBluetoothDevice.class);

    public BlueZBluetoothDevice(BlueZBridgeHandler adapter, BluetoothAddress address, String name) {
        super(adapter, address);
        this.name = name;
        logger.debug("Creating BlueZ device with address '{}'", address);
        tinyb.BluetoothDevice tinybDevice = findTinybDevice(address.toString());
        if (tinybDevice != null) {
            setTinybDevice(tinybDevice);
        }
    }

    public BlueZBluetoothDevice(BlueZBridgeHandler adapter, tinyb.BluetoothDevice tinybDevice) {
        super(adapter, new BluetoothAddress(tinybDevice.getAddress()));
        this.name = tinybDevice.getName();
        setTinybDevice(tinybDevice);
    }

    private tinyb.BluetoothDevice findTinybDevice(String address) {
        return ((BlueZBridgeHandler) getAdapter()).getTinyBAdapter().find(null, address);
    }

    public synchronized void setTinybDevice(tinyb.BluetoothDevice tinybDevice) {
        boolean init = (device == null);
        this.device = tinybDevice;
        this.rssi = (int) tinybDevice.getRSSI();
        this.txPower = (int) tinybDevice.getTxPower();
        if (init) {
            enableNotifications();
        }
    }

    private void enableNotifications() {
        logger.debug("Enabling notifications for device '{}'", device.getAddress());
        device.enableRSSINotifications(n -> {
            rssi = (int) n;
            BluetoothScanNotification notification = new BluetoothScanNotification();
            notification.setRssi(n);
            notifyListeners(BluetoothEventType.SCAN_RECORD, notification);
        });
        device.enableManufacturerDataNotifications(data -> {
            for (Short key : data.keySet()) {
                logger.info("{} -> {}", key, data.get(key));
            }
        });
        device.enableConnectedNotifications(connected -> {
            connectionState = connected ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED;
            logger.debug("Connection state of '{}' changed to {}", address, connectionState);
            notifyListeners(BluetoothEventType.CONNECTION_STATE,
                    new BluetoothConnectionStatusNotification(connectionState));
        });
        device.enableServicesResolvedNotifications(resolved -> {
            logger.debug("Received services resolved event for '{}': {}", address, resolved);
            if (resolved) {
                notifyListeners(BluetoothEventType.SERVICES_DISCOVERED);
            }
        });
    }

    @Override
    public boolean connect() {
        if (device != null) {
            boolean success = device.connect();
            return success;
        } else {
            return false;
        }
    }

}
