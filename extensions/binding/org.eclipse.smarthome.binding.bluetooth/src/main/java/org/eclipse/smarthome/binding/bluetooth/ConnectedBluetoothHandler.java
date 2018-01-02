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
package org.eclipse.smarthome.binding.bluetooth;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice.ConnectionState;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothConnectionStatusNotification;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a handler for generic Bluetooth devices in connected mode, which at the same time can be used
 * as a base implementation for more specific thing handlers.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.ARRAY_CONTENTS,
        DefaultLocation.TYPE_ARGUMENT, DefaultLocation.TYPE_BOUND, DefaultLocation.TYPE_PARAMETER })
public class ConnectedBluetoothHandler extends BeaconBluetoothHandler {

    private final Logger logger = LoggerFactory.getLogger(ConnectedBluetoothHandler.class);

    public ConnectedBluetoothHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        super.initialize();

        if (device.getConnectionState() != ConnectionState.CONNECTED) {
            // do the connecting async as it might take a while
            scheduler.submit(() -> {
                boolean success = device.connect();
                if (success) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                }
            });
        } else {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void dispose() {
        if (device != null) {
            device.disconnect();
        }
        super.dispose();
    }

    @Override
    public void onConnectionStateChange(BluetoothConnectionStatusNotification connectionNotification) {
        switch (connectionNotification.getConnectionState()) {
            case DISCOVERED:
                // The device is now known on the Bluetooth network, so we can do something...
                scheduler.submit(() -> {
                    if (!device.connect()) {
                        logger.debug("Error connecting to device after discovery.");
                    }
                });
                break;
            case CONNECTED:
                updateStatus(ThingStatus.ONLINE);
                scheduler.submit(() -> {
                    if (!device.discoverServices()) {
                        logger.debug("Error while discovering services");
                    }
                });
                break;
            case DISCONNECTED:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                break;
            default:
                break;
        }
    }

    @Override
    public void onServicesDiscovered() {
        logger.debug("Service discovery completed for '{}'", address);
        for (BluetoothService service : device.getServices()) {
            for (BluetoothCharacteristic characteristic : service.getCharacteristics()) {
                if (characteristic.getGattCharacteristic() != null) {
                    logger.debug("Added GATT characteristic '{}'", characteristic.getGattCharacteristic().name());
                }
            }
        }
    }

    @Override
    public void onCharacteristicReadComplete(BluetoothCharacteristic characteristic, BluetoothCompletionStatus status) {
        if (status == BluetoothCompletionStatus.SUCCESS) {
            logger.debug("Characteristic {} from {} has been read - value {}", characteristic.getUuid(), address,
                    characteristic.getValue());
        } else {
            logger.debug("Characteristic {} from {} has been read - ERROR", characteristic.getUuid(), address);
            return;
        }
    }

}
