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
package org.eclipse.smarthome.binding.bluetooth.bluegiga.internal.command.security;

import org.eclipse.smarthome.binding.bluetooth.bluegiga.internal.BlueGigaResponse;

/**
 * Class to implement the BlueGiga command <b>setBondableMode</b>.
 * <p>
 * This command is used to enter a passkey required for Man-in-the-Middle pairing. It should be
 * sent as a response to Passkey Request event.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public class BlueGigaSetBondableModeResponse extends BlueGigaResponse {
    public static int COMMAND_CLASS = 0x05;
    public static int COMMAND_METHOD = 0x01;

    /**
     * Response constructor
     */
    public BlueGigaSetBondableModeResponse(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
    }


    @Override
    public String toString() {
        return "BlueGigaSetBondableModeResponse []";
    }
}
