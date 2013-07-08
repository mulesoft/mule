package org.mule.api.context.notification;

import org.mule.context.notification.WatermarkNotification;


/**
 * <p>Listener of {@link WatermarkNotification}</p>
 *
 * @param <T> The {@link WatermarkNotification}
 * @since 3.5.0
 */
public interface WatermarkNotificationListener<T extends WatermarkNotification> extends ServerNotificationListener<WatermarkNotification>
{

}
