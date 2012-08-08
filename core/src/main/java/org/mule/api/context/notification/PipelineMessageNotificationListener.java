
package org.mule.api.context.notification;

import org.mule.context.notification.PipelineMessageNotification;

public interface PipelineMessageNotificationListener<T extends PipelineMessageNotification>
    extends ServerNotificationListener<PipelineMessageNotification>
{
}
