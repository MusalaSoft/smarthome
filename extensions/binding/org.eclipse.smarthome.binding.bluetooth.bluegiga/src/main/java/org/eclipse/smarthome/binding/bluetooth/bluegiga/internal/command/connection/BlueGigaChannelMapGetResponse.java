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
package org.eclipse.smarthome.binding.bluetooth.bluegiga.internal.command.connection;

import org.eclipse.smarthome.binding.bluetooth.bluegiga.internal.BlueGigaResponse;

/**
 * Class to implement the BlueGiga command <b>channelMapGet</b>.
 * <p>
 * This command can be used to read the current Channel Map.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public class BlueGigaChannelMapGetResponse extends BlueGigaResponse {
    public static int COMMAND_CLASS = 0x03;
    public static int COMMAND_METHOD = 0x04;

    /**
     * Connection handle.
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int connection;

    /**
     * Current Channel Map. Each bit corresponds to one channel. 0-bit corresponds to 0 channel.
     * Size of Channel Map is 5 bytes. Channel range: 0-36
     * <p>
     * BlueGiga API type is <i>uint8array</i> - Java type is {@link int[]}
     */
    private int[] map;

    /**
     * Response constructor
     */
    public BlueGigaChannelMapGetResponse(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        connection = deserializeUInt8();
        map = deserializeUInt8Array();
    }

    /**
     * Connection handle.
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current connection as {@link int}
     */
    public int getConnection() {
        return connection;
    }
    /**
     * Current Channel Map. Each bit corresponds to one channel. 0-bit corresponds to 0 channel.
     * Size of Channel Map is 5 bytes. Channel range: 0-36
     * <p>
     * BlueGiga API type is <i>uint8array</i> - Java type is {@link int[]}
     *
     * @return the current map as {@link int[]}
     */
    public int[] getMap() {
        return map;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaChannelMapGetResponse [connection=");
        builder.append(connection);
        builder.append(", map=");
        for (int c = 0; c < map.length; c++) {
            if (c > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", map[c]));
        }
        builder.append(']');
        return builder.toString();
    }
}
