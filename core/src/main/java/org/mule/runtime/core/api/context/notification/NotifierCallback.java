/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;

/**
 * Callback to actually perform the call to a {@link NotificationListener}.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface NotifierCallback {

  void notify(NotificationListener listener, Notification notification);
}
