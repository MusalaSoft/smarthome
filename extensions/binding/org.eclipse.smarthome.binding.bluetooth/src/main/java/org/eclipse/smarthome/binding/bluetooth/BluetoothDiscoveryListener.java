/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.bluetooth;

/**
 * This is a listener interface that is e.g. used by {@link BluetoothAdapter}s after discovering new devices.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public interface BluetoothDiscoveryListener {

    /**
     * Reports the discovery of a new device.
     *
     * @param device the newly discovered {@link BluetoothDevice}
     */
    void deviceDiscovered(BluetoothDevice device);

}
