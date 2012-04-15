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
