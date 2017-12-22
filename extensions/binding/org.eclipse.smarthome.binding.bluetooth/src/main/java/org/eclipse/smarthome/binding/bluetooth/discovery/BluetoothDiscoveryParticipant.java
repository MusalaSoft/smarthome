/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.bluetooth.discovery;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.binding.bluetooth.BluetoothDevice;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * A {@link BluetoothDiscoveryParticipant} that is registered as a service is picked up by the BluetoothDiscoveryService
 * and can thus contribute {@link DiscoveryResult}s from Bluetooth scans.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@NonNullByDefault
public interface BluetoothDiscoveryParticipant {

    /**
     * Defines the list of thing types that this participant can identify
     *
     * @return a set of thing type UIDs for which results can be created
     */
    public Set<ThingTypeUID> getSupportedThingTypeUIDs();

    /**
     * Creates a discovery result for a Bluetooth device
     *
     * @param device the Bluetooth device found on the network
     *
     * @return the according discovery result or <code>null</code>, if device is not
     *         supported by this participant
     */
    @Nullable
    public DiscoveryResult createResult(BluetoothDevice device);

    /**
     * Returns the thing UID for a Bluetooth device
     *
     * @param device the Bluetooth device
     *
     * @return a thing UID or <code>null</code>, if the device is not supported by this participant
     */
    @Nullable
    public ThingUID getThingUID(BluetoothDevice device);
}
