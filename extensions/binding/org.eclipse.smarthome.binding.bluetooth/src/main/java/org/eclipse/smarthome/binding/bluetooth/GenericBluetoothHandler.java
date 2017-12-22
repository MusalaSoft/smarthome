/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.bluetooth;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothConnectionStatusNotification;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothScanNotification;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a handler for generic Bluetooth devices, which at the same time can be used
 * as a base implementation for more specific thing handlers.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.ARRAY_CONTENTS,
        DefaultLocation.TYPE_ARGUMENT, DefaultLocation.TYPE_BOUND, DefaultLocation.TYPE_PARAMETER })
public class GenericBluetoothHandler extends BaseThingHandler implements BluetoothDeviceListener {

    private final Logger logger = LoggerFactory.getLogger(GenericBluetoothHandler.class);

    // The Bluetooth adapter we use for communication
    protected BluetoothAdapter adapter;

    // Our Bluetooth address
    protected BluetoothAddress address;

    // Our device
    protected BluetoothDevice device;

    public GenericBluetoothHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        try {
            address = new BluetoothAddress(getConfig().get(BluetoothBindingConstants.CONFIGURATION_ADDRESS).toString());
        } catch (IllegalArgumentException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getLocalizedMessage());
            return;
        }

        updateStatus(ThingStatus.ONLINE);

        BridgeHandler bridgeHandler = getBridge().getHandler();
        if (!(bridgeHandler instanceof BluetoothAdapter)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Associated with an unsupported bridge");
            return;
        }

        adapter = (BluetoothAdapter) bridgeHandler;
        device = adapter.getDevice(address);
        device.addListener(this);
    }

    @Override
    public void dispose() {
        device.removeListener(this);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            Integer rssi = device.getRssi();
            if (rssi != null) {
                updateState(BluetoothBindingConstants.CHANNEL_TYPE_RSSI, new DecimalType(rssi));
            }
        }
    }

    @Override
    public void onScanRecordReceived(BluetoothScanNotification scanNotification) {
        updateState(BluetoothBindingConstants.CHANNEL_TYPE_RSSI, new DecimalType(scanNotification.getRssi()));
    }

    @Override
    public void onConnectionStateChange(BluetoothConnectionStatusNotification connectionNotification) {
        switch (connectionNotification.getConnectionState()) {
            case DISCOVERED:
                // The device is now known on the Bluetooth network, so we can do something...
                if (!device.connect()) {
                    logger.debug("Error connecting to device after discovery");
                }
                break;
            case CONNECTED:
                if (!device.discoverServices()) {
                    logger.debug("Error while discovering services");
                }
                break;
            case DISCONNECTED:
                break;
            default:
                break;
        }
    }

    @Override
    public void onServicesDiscovered() {
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

    @Override
    public void onCharacteristicWriteComplete(BluetoothCharacteristic characteristic,
            BluetoothCompletionStatus status) {
    }

    @Override
    public void onCharacteristicUpdate(BluetoothCharacteristic characteristic) {
    }

}
