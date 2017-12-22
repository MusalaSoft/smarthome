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
package org.eclipse.smarthome.binding.bluetooth.bluegiga.internal.command.attributedb;

import org.eclipse.smarthome.binding.bluetooth.bluegiga.internal.BlueGigaCommand;

/**
 * Class to implement the BlueGiga command <b>readType</b>.
 * <p>
 * This command reads the given attribute's type (UUID) from the local database.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public class BlueGigaReadTypeCommand extends BlueGigaCommand {
    public static int COMMAND_CLASS = 0x02;
    public static int COMMAND_METHOD = 0x02;

    /**
     * Handle of the attribute to read
     * <p>
     * BlueGiga API type is <i>uint16</i> - Java type is {@link int}
     */
    private int handle;

    /**
     * Handle of the attribute to read
     *
     * @param handle the handle to set as {@link int}
     */
    public void setHandle(int handle) {
        this.handle = handle;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeUInt16(handle);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaReadTypeCommand [handle=");
        builder.append(handle);
        builder.append(']');
        return builder.toString();
    }
}
