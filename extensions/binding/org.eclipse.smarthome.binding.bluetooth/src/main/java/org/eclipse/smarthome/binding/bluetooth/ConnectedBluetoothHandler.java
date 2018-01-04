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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.binding.bluetooth.BluetoothCharacteristic.GattCharacteristic;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice.ConnectionState;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothConnectionStatusNotification;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.DefaultSystemChannelTypeProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
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
    private ScheduledFuture<?> connectionJob;

    public ConnectedBluetoothHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        super.initialize();

        connectionJob = scheduler.scheduleWithFixedDelay(() -> {
            if (device.getConnectionState() != ConnectionState.CONNECTED) {
                device.connect();
                // we do not set the Thing status here, because we will anyhow receive a call to onConnectionStateChange
            } else {
                updateStatus(ThingStatus.ONLINE);
            }
            updateRSSI();
        }, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        if (connectionJob != null) {
            connectionJob.cancel(true);
        }
        if (device != null) {
            device.disconnect();
        }
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);
        if (channelUID.getId().equals(GattCharacteristic.BATTERY_LEVEL.name()) && command == RefreshType.REFRESH) {
            BluetoothCharacteristic characteristic = device
                    .getCharacteristic(GattCharacteristic.BATTERY_LEVEL.getUUID());
            if (characteristic != null) {
                device.readCharacteristic(characteristic);
            }
        }
    }

    @Override
    protected void updateStatusBasedOnRssi(boolean receivedSignal) {
        // if there is no signal, we can be sure we are OFFLINE, but if there is a signal, we also have to check whether
        // we are connected.
        if (receivedSignal) {
            if (device.getConnectionState() == ConnectionState.CONNECTED) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Device is not connected.");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
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
                    if (characteristic.getGattCharacteristic().equals(GattCharacteristic.BATTERY_LEVEL)) {
                        addChannel(GattCharacteristic.BATTERY_LEVEL,
                                DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_BATTERY_LEVEL);
                        device.enableNotifications(characteristic);
                        continue;
                    }
                    logger.debug("Added GATT characteristic '{}'", characteristic.getGattCharacteristic().name());
                }
            }
        }
    }

    @Override
    public void onCharacteristicReadComplete(BluetoothCharacteristic characteristic, BluetoothCompletionStatus status) {
        if (status == BluetoothCompletionStatus.SUCCESS) {
            if (characteristic.getGattCharacteristic().equals(GattCharacteristic.BATTERY_LEVEL)) {
                updateBatteryLevel(characteristic);
            } else {
                logger.debug("Characteristic {} from {} has been read - value {}", characteristic.getUuid(), address,
                        characteristic.getValue());
            }
        } else {
            logger.debug("Characteristic {} from {} has been read - ERROR", characteristic.getUuid(), address);
            return;
        }
    }

    @Override
    public void onCharacteristicUpdate(BluetoothCharacteristic characteristic) {
        if (characteristic.getGattCharacteristic().equals(GattCharacteristic.BATTERY_LEVEL)) {
            updateBatteryLevel(characteristic);
        }
    }

    protected void updateBatteryLevel(BluetoothCharacteristic characteristic) {
        // the byte has values from 0-255, which we need to map to 0-100
        Double level = characteristic.getValue()[0] / 2.55;
        updateState(characteristic.getGattCharacteristic().name(), new DecimalType(level.intValue()));
    }

    private void addChannel(GattCharacteristic characteristic, ChannelType channelType) {
        if (getThing().getChannel(characteristic.name()) == null) {
            ThingBuilder updatedThing = editThing();
            Channel channel = ChannelBuilder
                    .create(new ChannelUID(getThing().getUID(), characteristic.name()), "Number")
                    .withType(channelType.getUID()).withLabel(channelType.getLabel()).build();
            updatedThing.withChannel(channel);
            updateThing(updatedThing.build());
            logger.debug("Added channel '{}' to Thing '{}'", channel.getLabel(), getThing().getUID());
        }
    }

}
