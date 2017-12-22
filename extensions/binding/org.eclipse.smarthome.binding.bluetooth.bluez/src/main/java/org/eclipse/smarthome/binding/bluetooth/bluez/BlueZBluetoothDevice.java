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

import java.util.List;

import org.eclipse.smarthome.binding.bluetooth.BluetoothAddress;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice;
import org.eclipse.smarthome.binding.bluetooth.bluez.handler.BlueZBridgeHandler;
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
        initDevice();
    }

    public BlueZBluetoothDevice(BlueZBridgeHandler adapter, tinyb.BluetoothDevice tinybDevice) {
        super(adapter, new BluetoothAddress(tinybDevice.getAddress()));
        this.name = tinybDevice.getName();
        this.device = tinybDevice;
    }

    private synchronized void initDevice() {
        if (device == null) {
            List<tinyb.BluetoothDevice> devices = ((BlueZBridgeHandler) getAdapter()).getTinyBAdapter().getDevices();
            device = devices.stream().filter(d -> d.getAddress().equals(getAddress().toString())).findFirst()
                    .orElse(null);
        }
    }

    public void activateSubscriptions() {
        device.enableRSSINotifications(n -> {
            rssi = (int) n;
            BluetoothScanNotification notification = new BluetoothScanNotification();
            notification.setRssi(n);
            notifyListeners(BluetoothEventType.SCAN_RECORD, notification);
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
