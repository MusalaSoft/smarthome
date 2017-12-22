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
package org.eclipse.smarthome.binding.bluetooth.bluegiga.internal.enumeration;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to implement the BlueGiga Enumeration <b>SmpIoCapabilities</b>.
 * <p>
 * Security Manager I/O Capabilities
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public enum SmpIoCapabilities {
    /**
     * Default unknown value
     */
    UNKNOWN(-1),

    /**
     * [0] Display Only
     */
    SM_IO_CAPABILITY_DISPLAYONLY(0x0000),

    /**
     * [1] Display with Yes/No-buttons
     */
    SM_IO_CAPABILITY_DISPLAYYESNO(0x0001),

    /**
     * [2] Keyboard Only
     */
    SM_IO_CAPABILITY_KEYBOARDONLY(0x0002),

    /**
     * [3] No Input and No Output
     */
    SM_IO_CAPABILITY_NOINPUTNOOUTPUT(0x0003),

    /**
     * [4] Display with Keyboard
     */
    SM_IO_CAPABILITY_KEYBOARDDISPLAY(0x0004);

    /**
     * A mapping between the integer code and its corresponding type to
     * facilitate lookup by code.
     */
    private static Map<Integer, SmpIoCapabilities> codeMapping;

    private int key;

    private SmpIoCapabilities(int key) {
        this.key = key;
    }

    private static void initMapping() {
        codeMapping = new HashMap<Integer, SmpIoCapabilities>();
        for (SmpIoCapabilities s : values()) {
            codeMapping.put(s.key, s);
        }
    }

    /**
     * Lookup function based on the type code. Returns null if the code does not exist.
     *
     * @param smpIoCapabilities
     *            the code to lookup
     * @return enumeration value.
     */
    public static SmpIoCapabilities getSmpIoCapabilities(int smpIoCapabilities) {
        if (codeMapping == null) {
            initMapping();
        }

        if (codeMapping.get(smpIoCapabilities) == null) {
            return UNKNOWN;
        }

        return codeMapping.get(smpIoCapabilities);
    }

    /**
     * Returns the BlueGiga protocol defined value for this enum
     *
     * @return the BGAPI enumeration key
     */
    public int getKey() {
        return key;
    }
}
