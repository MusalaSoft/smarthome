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

/**
 * The {@link BluetoothBridge} class defines the standard bridge API that must be implemented by Thing implemented in the
 * BLE host.
 * <p>
 * <b>Scanning</b>
 * The API assumes that the bridge is "always" scanning to enable beacons to be received.
 * The bridge must decide to enable and disable scanning as it needs. This design choice avoids interaction between
 * higher layers where a binding may want to enable scanning while another needs to disable scanning for a specific
 * function (e.g. to connect to a device). The bridge should disable scanning only for the period that is needed.
 *
 * @author Chris Jackson - Initial contribution
 */
public interface BluetoothBridge {

    /**
     * Starts an active scan on the Bluetooth interface.
     *
     * @return true if the scan was started
     */
    boolean scanStart();

    /**
     * Stops an active scan on the Bluetooth interface
     */
    void scanStop();

    /**
     * Gets the {@link BluetoothAddress} of the bridge
     *
     * @return the {@link BluetoothAddress} of the bridge
     */
    BluetoothAddress getAddress();

    /**
     * Gets the {@link BluetoothDevice} given the {@link BluetoothAddress}.
     * A {@link BluetoothDevice} will always be returned for a valid hardware address, even if this adapter has never seen
     * that device.
     *
     * @param address the {@link BluetoothAddress} to retrieve
     * @return the {@link BluetoothDevice}
     */
    BluetoothDevice getDevice(BluetoothAddress address);

}
