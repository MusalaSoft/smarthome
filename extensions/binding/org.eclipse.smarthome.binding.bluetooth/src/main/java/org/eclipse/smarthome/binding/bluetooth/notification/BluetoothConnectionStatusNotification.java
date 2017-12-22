/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.bluetooth.notification;

import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice.ConnectionState;

/**
 * The {@link BluetoothConnectionStatusNotification} provides a notification of a change in the device connection state.
 *
 * @author Chris Jackson - Initial contribution
 */
public class BluetoothConnectionStatusNotification extends BluetoothNotification {
    private ConnectionState connectionState;

    public BluetoothConnectionStatusNotification(ConnectionState connectionState) {
        this.connectionState = connectionState;
    }

    /**
     * Returns the connection state for this notification
     * 
     * @return the {@link ConnectionState}
     */
    public ConnectionState getConnectionState() {
        return connectionState;
    };
}
