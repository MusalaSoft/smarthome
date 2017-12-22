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

import org.eclipse.smarthome.binding.bluetooth.bluegiga.internal.BlueGigaCommand;

/**
 * Class to implement the BlueGiga command <b>passKey</b>.
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
public class BlueGigaPassKeyCommand extends BlueGigaCommand {
    public static int COMMAND_CLASS = 0x05;
    public static int COMMAND_METHOD = 0x04;

    /**
     * Connection handle
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int handle;

    /**
     * Passkey. Range: 000000-999999
     * <p>
     * BlueGiga API type is <i>uint32</i> - Java type is {@link long}
     */
    private long passkey;

    /**
     * Connection handle
     *
     * @param handle the handle to set as {@link int}
     */
    public void setHandle(int handle) {
        this.handle = handle;
    }
    /**
     * Passkey. Range: 000000-999999
     *
     * @param passkey the passkey to set as {@link long}
     */
    public void setPasskey(long passkey) {
        this.passkey = passkey;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeUInt8(handle);
        serializeUInt32(passkey);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaPassKeyCommand [handle=");
        builder.append(handle);
        builder.append(", passkey=");
        builder.append(passkey);
        builder.append(']');
        return builder.toString();
    }
}
