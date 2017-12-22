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
package org.eclipse.smarthome.binding.bluetooth.bluez.handler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.binding.bluetooth.BluetoothAdapter;
import org.eclipse.smarthome.binding.bluetooth.BluetoothAddress;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDiscoveryListener;
import org.eclipse.smarthome.binding.bluetooth.bluez.BlueZAdapterConstants;
import org.eclipse.smarthome.binding.bluetooth.bluez.BlueZBluetoothDevice;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tinyb.BluetoothManager;

/**
 * The {@link BlueZBridgeHandler} is responsible for talking to the BlueZ stack.
 * It provides a private interface for {@link BlueZBluetoothDevice}s to access the stack and provides top
 * level adaptor functionality for scanning and arbitration.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
public class BlueZBridgeHandler extends BaseBridgeHandler implements BluetoothAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BlueZBridgeHandler.class);

    private tinyb.BluetoothAdapter adapter;

    // Our BT address
    private BluetoothAddress address;

    // Map of Bluetooth devices known to this bridge.
    // This is all devices we have heard on the network - not just things bound to the bridge
    private final Map<String, BluetoothDevice> devices = new ConcurrentHashMap<>();

    // Set of discovery listeners
    protected final Set<BluetoothDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();

    private ScheduledFuture<?> discoveryJob;

    public BlueZBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        Object cfgAddress = getConfig().get(BlueZAdapterConstants.PROPERTY_ADDRESS);
        if (cfgAddress != null) {
            address = new BluetoothAddress(cfgAddress.toString());
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "address not set");
            return;
        }
        logger.debug("Creating BlueZ adapter with address '{}'", address);
        for (tinyb.BluetoothAdapter a : BluetoothManager.getBluetoothManager().getAdapters()) {
            if (a.getAddress().equals(address.toString())) {
                adapter = a;
                updateStatus(ThingStatus.ONLINE);
                discoveryJob = scheduler.scheduleWithFixedDelay(() -> {
                    checkForNewDevices();
                }, 0, 10, TimeUnit.SECONDS);
                return;
            }
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No adapter for this address found.");
    }

    @Override
    public ThingUID getUID() {
        return getThing().getUID();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
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
        adapter.startDiscovery();
        for (tinyb.BluetoothDevice tinybDevice : adapter.getDevices()) {
            BluetoothDevice device = getDevice(new BluetoothAddress(tinybDevice.getAddress()));
            notifyEventListeners(device);
        }
    }

    @Override
    public void scanStop() {
        adapter.stopDiscovery();
    }

    @Override
    public BluetoothAddress getAddress() {
        return address;
    }

    @Override
    public BluetoothDevice getDevice(BluetoothAddress address) {
        if (devices.containsKey(address.toString())) {
            return devices.get(address.toString());
        } else {
            BluetoothDevice device = new BlueZBluetoothDevice(this, address, "");
            devices.put(address.toString(), device);
            return device;
        }
    }

    @Override
    public void dispose() {
        if (discoveryJob != null) {
            discoveryJob.cancel(true);
            discoveryJob = null;
        }
    }

    public tinyb.BluetoothAdapter getTinyBAdapter() {
        return adapter;
    }

    private void checkForNewDevices() {
        for (tinyb.BluetoothDevice tinybDevice : adapter.getDevices()) {
            if (!devices.containsKey(tinybDevice.getAddress())) {
                createAndRegisterBlueZDevice(tinybDevice);
            }
        }
    }

    private void createAndRegisterBlueZDevice(tinyb.BluetoothDevice tinybDevice) {
        BlueZBluetoothDevice device = new BlueZBluetoothDevice(this, tinybDevice);
        devices.put(tinybDevice.getAddress(), device);
        notifyEventListeners(device);
        device.activateSubscriptions();
    }

    private void notifyEventListeners(BluetoothDevice device) {
        for (BluetoothDiscoveryListener listener : discoveryListeners) {
            listener.deviceDiscovered(device);
        }
    }

}
