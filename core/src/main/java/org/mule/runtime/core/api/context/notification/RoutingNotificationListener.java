/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

/**
 * <code>RoutingNotificationListener</code> is an observer interface that objects can use to receive notifications about routing
 * events such as async-reply misses.
 */
public interface RoutingNotificationListener<T extends RoutingNotification>
    extends NotificationListener<RoutingNotification> {
  // no methods
}
