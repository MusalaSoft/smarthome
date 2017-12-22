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
package org.eclipse.smarthome.binding.bluetooth.bluegiga.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link NotificationService} is used to provide notifications to our listeners "safely". A separate
 * thread is used so that the notifier is not blocked.
 * <p>
 * This helper class ensures that the stack handles threads efficiently throughout the system.
 *
 * @author Chris Jackson
 */
public class NotificationService {
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static void execute(Runnable command) {
        executorService.execute(command);
    }

}
