/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.bluetooth.bluez.internal.dbus;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;

@DBusInterfaceName("org.bluez.ThermometerManager1")
public interface ThermometerManager1 extends DBusInterface {

    public void RegisterWatcher(DBusInterface agent);

    public void UnregisterWatcher(DBusInterface agent);

    public void EnableIntermediateMeasurement(DBusInterface agent);

    public void DisableIntermediateMeasurement(DBusInterface agent);

}
