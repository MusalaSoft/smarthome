/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.bluetooth.notification;

import org.eclipse.smarthome.binding.bluetooth.BluetoothAddress;

/**
 * The {@link BluetoothNotification} is the base class for BLE device notifications
 *
 * @author Chris Jackson - Initial contribution
 */
public abstract class BluetoothNotification {
    protected BluetoothAddress address;

    /**
     * Returns the bluetooth address for this frame
     */
    public BluetoothAddress getAddress() {
        return address;
    }
}
