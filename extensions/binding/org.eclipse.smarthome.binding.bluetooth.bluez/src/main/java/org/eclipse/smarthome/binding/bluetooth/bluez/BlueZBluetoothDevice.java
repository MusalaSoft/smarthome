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
import java.util.Optional;

import org.eclipse.smarthome.binding.bluetooth.BluetoothAddress;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice;
import org.eclipse.smarthome.binding.bluetooth.bluez.handler.BlueZBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BluetoothDevice for BlueZ via TinyB
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class BlueZBluetoothDevice extends BluetoothDevice {

    private Optional<tinyb.BluetoothDevice> device;

    private static final Logger logger = LoggerFactory.getLogger(BlueZBluetoothDevice.class);

    public BlueZBluetoothDevice(BlueZBridgeHandler adapter, BluetoothAddress address, String name) {
        super(adapter, address);
        this.name = name;
        logger.debug("Creating BlueZ device with address '{}'", address);
        initDevice();
    }

    private synchronized void initDevice() {
        if (device == null) {
            List<tinyb.BluetoothDevice> devices = ((BlueZBridgeHandler) getAdapter()).getTinyBAdapter().getDevices();
            device = devices.stream().filter(d -> d.getAddress().equals(getAddress().toString())).findFirst();
        }
    }

    @Override
    public boolean connect() {
        if (device.isPresent()) {
            return device.get().connect();
        } else {
            return false;
        }
    }

}
