/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
