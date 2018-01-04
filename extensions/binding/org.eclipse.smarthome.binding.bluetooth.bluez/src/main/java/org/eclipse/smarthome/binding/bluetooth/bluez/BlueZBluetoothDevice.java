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
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.binding.bluetooth.BluetoothAddress;
import org.eclipse.smarthome.binding.bluetooth.BluetoothCharacteristic;
import org.eclipse.smarthome.binding.bluetooth.BluetoothCompletionStatus;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDescriptor;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice;
import org.eclipse.smarthome.binding.bluetooth.BluetoothService;
import org.eclipse.smarthome.binding.bluetooth.bluez.handler.BlueZBridgeHandler;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothConnectionStatusNotification;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothScanNotification;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tinyb.BluetoothException;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattDescriptor;
import tinyb.BluetoothGattService;

/**
 * Implementation of BluetoothDevice for BlueZ via TinyB
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class BlueZBluetoothDevice extends BluetoothDevice {

    private tinyb.BluetoothDevice device;

    private final Logger logger = LoggerFactory.getLogger(BlueZBluetoothDevice.class);

    private final ExecutorService executor = ThreadPoolManager.getPool("bluetooth");

    public BlueZBluetoothDevice(BlueZBridgeHandler adapter, BluetoothAddress address, String name) {
        super(adapter, address);
        this.name = name;
        logger.debug("Creating BlueZ device with address '{}'", address);
        executor.submit(() -> {
            tinyb.BluetoothDevice tinybDevice = findTinybDevice(address.toString());
            if (tinybDevice != null) {
                setTinybDevice(tinybDevice);
            }
        });
    }

    public BlueZBluetoothDevice(BlueZBridgeHandler adapter, tinyb.BluetoothDevice tinybDevice) {
        super(adapter, new BluetoothAddress(tinybDevice.getAddress()));
        this.name = tinybDevice.getName();
        executor.submit(() -> setTinybDevice(tinybDevice));
    }

    private tinyb.@Nullable BluetoothDevice findTinybDevice(String address) {
        List<tinyb.BluetoothDevice> deviceList = ((BlueZBridgeHandler) getAdapter()).getTinyBAdapter().getDevices();
        logger.trace("Searching for '{}' in {} devices.", address, deviceList.size());
        return deviceList.stream().filter(d -> d.getAddress().equals(address)).findFirst().orElse(null);
    }

    public synchronized void setTinybDevice(tinyb.BluetoothDevice tinybDevice) {
        if (device == null) {
            this.device = tinybDevice;
            enableNotifications();
        } else if (!device.equals(tinybDevice)) {
            // we need to replace the instance - this should never happen for any connected device.
            disableNotifications();
            this.device = tinybDevice;
            enableNotifications();
        }
        this.rssi = (int) this.device.getRSSI();
        this.txPower = (int) this.device.getTxPower();
        if (this.device.getConnected()) {
            this.connectionState = ConnectionState.CONNECTED;
        }
        refreshServices();
    }

    private void enableNotifications() {
        logger.debug("Enabling notifications for device '{}'", device.getAddress());
        device.enableRSSINotifications(n -> {
            rssi = (int) n;
            BluetoothScanNotification notification = new BluetoothScanNotification();
            notification.setRssi(n);
            notifyListeners(BluetoothEventType.SCAN_RECORD, notification);
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
                refreshServices();
                notifyListeners(BluetoothEventType.SERVICES_DISCOVERED);
            }
        });
        device.enableServiceDataNotifications(data -> {
            logger.debug("Received service data for '{}': {}", address, data);
        });
    }

    private void disableNotifications() {
        logger.debug("Disabling notifications for device '{}'", device.getAddress());
        device.disableBlockedNotifications();
        device.disableManufacturerDataNotifications();
        device.disablePairedNotifications();
        device.disableRSSINotifications();
        device.disableServiceDataNotifications();
        device.disableTrustedNotifications();
    }

    protected void refreshServices() {
        if (device.getServices().size() > getServices().size()) {
            for (BluetoothGattService tinybService : device.getServices()) {
                BluetoothService service = new BluetoothService(UUID.fromString(tinybService.getUUID()),
                        tinybService.getPrimary());
                for (BluetoothGattCharacteristic tinybCharacteristic : tinybService.getCharacteristics()) {
                    BluetoothCharacteristic characteristic = new BluetoothCharacteristic(
                            UUID.fromString(tinybCharacteristic.getUUID()), 0);
                    for (BluetoothGattDescriptor tinybDescriptor : tinybCharacteristic.getDescriptors()) {
                        BluetoothDescriptor descriptor = new BluetoothDescriptor(characteristic,
                                UUID.fromString(tinybDescriptor.getUUID()));
                        characteristic.addDescriptor(descriptor);
                    }
                    service.addCharacteristic(characteristic);
                }
                addService(service);
            }
            notifyListeners(BluetoothEventType.SERVICES_DISCOVERED);
        }
    }

    @Override
    public boolean connect() {
        if (device != null && !device.getConnected()) {
            try {
                return device.connect();
            } catch (BluetoothException e) {
                if ("Timeout was reached".equals(e.getMessage())) {
                    notifyListeners(BluetoothEventType.CONNECTION_STATE,
                            new BluetoothConnectionStatusNotification(ConnectionState.DISCONNECTED));
                } else if (e.getMessage() != null && e.getMessage().contains("Protocol not available")) {
                    // this device does not seem to be connectable at all - let's log a warning and ignore it.
                    logger.warn("Bluetooth device '{}' does not allow a connection.", device.getAddress());
                } else {
                    logger.debug("Exception occurred when trying to connect device '{}'", device.getAddress(), e);
                }
            }
        }
        return false;
    }

    @Override
    public boolean disconnect() {
        if (device != null && device.getConnected()) {
            logger.debug("Disconnecting '{}'", address);
            try {
                return device.disconnect();
            } catch (BluetoothException e) {
                logger.debug("Exception occurred when trying to disconnect device '{}'", device.getAddress(), e);
            }
        }
        return false;
    }

    @Override
    public boolean readCharacteristic(BluetoothCharacteristic characteristic) {
        BluetoothGattCharacteristic c = getTinybCharacteristicByUUID(characteristic.getUuid().toString());
        executor.submit(() -> {
            try {
                byte[] value = c.readValue();
                characteristic.setValue(value);
                notifyListeners(BluetoothEventType.CHARACTERISTIC_READ_COMPLETE, characteristic,
                        BluetoothCompletionStatus.SUCCESS);
            } catch (BluetoothException e) {
                logger.debug("Exception occurred when trying to read characteristic '{}': {}", characteristic.getUuid(),
                        e.getMessage());
                notifyListeners(BluetoothEventType.CHARACTERISTIC_READ_COMPLETE, characteristic,
                        BluetoothCompletionStatus.ERROR);
            }
        });
        return true;
    }

    @Override
    public boolean writeCharacteristic(BluetoothCharacteristic characteristic) {
        BluetoothGattCharacteristic c = getTinybCharacteristicByUUID(characteristic.getUuid().toString());
        executor.submit(() -> {
            try {
                BluetoothCompletionStatus successStatus = c.writeValue(characteristic.getByteValue())
                        ? BluetoothCompletionStatus.SUCCESS
                        : BluetoothCompletionStatus.ERROR;
                notifyListeners(BluetoothEventType.CHARACTERISTIC_WRITE_COMPLETE, characteristic, successStatus);
            } catch (BluetoothException e) {
                logger.debug("Exception occurred when trying to read characteristic '{}': {}", characteristic.getUuid(),
                        e.getMessage());
                notifyListeners(BluetoothEventType.CHARACTERISTIC_WRITE_COMPLETE, characteristic,
                        BluetoothCompletionStatus.ERROR);
            }
        });
        return true;
    }

    @Override
    public boolean enableNotifications(BluetoothCharacteristic characteristic) {
        BluetoothGattCharacteristic c = getTinybCharacteristicByUUID(characteristic.getUuid().toString());
        if (c != null) {
            try {
                c.enableValueNotifications(value -> {
                    logger.debug("Received new value '{}' for characteristic '{}' of device '{}'", value,
                            characteristic.getUuid(), address);
                    characteristic.setValue(value);
                    notifyListeners(BluetoothEventType.CHARACTERISTIC_UPDATED, characteristic);
                });
            } catch (BluetoothException e) {
                if (e.getMessage().contains("Already notifying")) {
                    return false;
                } else {
                    logger.warn("Exception occurred while activating notifications on '{}'", address, e);
                }
            }
            return true;
        } else {
            logger.warn("Characteristic '{}' is missing on device '{}'.", characteristic.getUuid(), address);
            return false;
        }
    }

    @Override
    public boolean disableNotifications(BluetoothCharacteristic characteristic) {
        BluetoothGattCharacteristic c = getTinybCharacteristicByUUID(characteristic.getUuid().toString());
        if (c != null) {
            c.disableValueNotifications();
            return true;
        } else {
            logger.warn("Characteristic '{}' is missing on device '{}'.", characteristic.getUuid(), address);
            return false;
        }
    }

    @Override
    public boolean enableNotifications(BluetoothDescriptor descriptor) {
        BluetoothGattDescriptor d = getTinybDescriptorByUUID(descriptor.getUuid().toString());
        if (d != null) {
            d.enableValueNotifications(value -> {
                logger.debug("Received new value '{}' for descriptor '{}' of device '{}'", value, descriptor.getUuid(),
                        address);
                descriptor.setValue(value);
                notifyListeners(BluetoothEventType.DESCRIPTOR_UPDATED, descriptor);
            });
            return true;
        } else {
            logger.warn("Descriptor '{}' is missing on device '{}'.", descriptor.getUuid(), address);
            return false;
        }
    }

    @Override
    public boolean disableNotifications(BluetoothDescriptor descriptor) {
        BluetoothGattDescriptor d = getTinybDescriptorByUUID(descriptor.getUuid().toString());
        if (d != null) {
            d.disableValueNotifications();
            return true;
        } else {
            logger.warn("Descriptor '{}' is missing on device '{}'.", descriptor.getUuid(), address);
            return false;
        }
    }

    private BluetoothGattCharacteristic getTinybCharacteristicByUUID(String uuid) {
        for (BluetoothGattService service : device.getServices()) {
            for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                if (c.getUUID().equals(uuid)) {
                    return c;
                }
            }
        }
        return null;
    }

    private BluetoothGattDescriptor getTinybDescriptorByUUID(String uuid) {
        for (BluetoothGattService service : device.getServices()) {
            for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                for (BluetoothGattDescriptor d : c.getDescriptors()) {
                    if (d.getUUID().equals(uuid)) {
                        return d;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Clean up and release memory.
     */
    public void dispose() {
        disableNotifications();
    }
}
