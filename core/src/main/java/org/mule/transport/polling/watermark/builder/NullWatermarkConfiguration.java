package org.mule.transport.polling.watermark.builder;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;


/**
 * Null Pattern. If no watermark configuration is defined in the xml, then this class is instantiated.
 */
public class NullWatermarkConfiguration implements WatermarkConfiguration
{

    /**
     * Just returns the same message source configured in the poll inbound endpoint
     */
    @Override
    public MessageProcessor buildMessageSourceFrom(MessageProcessor processor)
    {
        return processor;
    }

    /**
     * Does not register any listener.
     */
    @Override
    public void registerPipelineNotificationListener(FlowConstruct flowConstruct)
    {
        // Does nothing
    }


}
