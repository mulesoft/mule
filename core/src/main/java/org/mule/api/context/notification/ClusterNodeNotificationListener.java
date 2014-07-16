/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import org.mule.context.notification.ClusterNodeNotification;

/**
 * Defines a listener for {@link ClusterNodeNotification}
 *
 * @param <T> cluster node notification type
 */
public interface ClusterNodeNotificationListener<T extends ClusterNodeNotification> extends ServerNotificationListener<ClusterNodeNotification>
{

}
