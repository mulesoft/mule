package org.mule.transport.polling.watermark.builder;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;

/**
 * The watermark configuration defined in the poll inbound endpoint.
 * This class works as a builder of the {@link org.mule.transport.polling.watermark.WatermarkRetrieveMessageProcessor}
 * and a register of the {@link org.mule.transport.polling.watermark.WatermarkStorePipelineListener}
 */
public interface WatermarkConfiguration
{

    /**
     * Creates the Poll receiver source based on the configured poll source. The user will configure a source inside
     * the poll inbound endpoint. The {@link WatermarkConfiguration} will decorate that message source with
     * the {@link org.mule.transport.polling.watermark.WatermarkRetrieveMessageProcessor}
     *
     * @param processor The message source configured by the user.
     * @return The decorated Message source with the {@link org.mule.transport.polling.watermark.WatermarkRetrieveMessageProcessor}
     */
    MessageProcessor buildMessageSourceFrom(MessageProcessor processor);

    /**
     * Registers the {@link org.mule.transport.polling.watermark.WatermarkStorePipelineListener} as
     * listener of the {@link org.mule.api.context.notification.PipelineMessageNotificationListener}
     *
     * @param flowConstruct The listener will act at the {@link org.mule.context.notification.PipelineMessageNotification#PROCESS_END}
     *                      action. We need the flowConstruct to identify of which flow we need to process the notification
     */
    void registerPipelineNotificationListener(FlowConstruct flowConstruct);
}
