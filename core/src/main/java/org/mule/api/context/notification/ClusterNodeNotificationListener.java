/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
