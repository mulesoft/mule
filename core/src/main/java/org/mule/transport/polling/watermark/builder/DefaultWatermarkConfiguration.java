package org.mule.transport.polling.watermark.builder;


import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.store.ObjectStore;
import org.mule.context.notification.NotificationException;
import org.mule.processor.chain.SimpleMessageProcessorChainBuilder;
import org.mule.transport.polling.watermark.WatermarkRetrieveMessageProcessor;
import org.mule.transport.polling.watermark.WatermarkStorePipelineListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * The default configuration of the watermark. If the user defines a watermark configuration in the xml, then this class
 * is instantiated
 */
public class DefaultWatermarkConfiguration implements WatermarkConfiguration, MuleContextAware
{

    /**
     * Logger to notify errors.
     */
    private static Log logger = LogFactory.getLog(DefaultWatermarkConfiguration.class);

    /**
     * The watermark variable that will end up being the object store key. This variable is also the name of the flow
     * variable in the flow construct.
     */
    private String variable;

    /**
     * The default expression to update the flow variable in case it is not in the object store or it fails to retrieve
     * it.
     */
    private String defaultExpression;

    /**
     * The update expression to update the watermark value in the object store.
     * It is optional so it can be null.
     */
    private String updateExpression;

    /**
     * The object store instance.
     * The default value is the persistent user object store.
     */
    private ObjectStore objectStore;

    /**
     * The mule context instance
     */
    private MuleContext muleContext;

    /**
     * If there is a message source configured it creates a chain of message processors where the first message processor
     * is the {@link org.mule.transport.polling.watermark.WatermarkRetrieveMessageProcessor}
     *
     * @see WatermarkConfiguration#buildMessageSourceFrom(org.mule.api.processor.MessageProcessor)
     */
    @Override
    public MessageProcessor buildMessageSourceFrom(MessageProcessor processor)
    {
        WatermarkRetrieveMessageProcessor watermarkSource =
                new WatermarkRetrieveMessageProcessor(muleContext, objectStore, variable, defaultExpression);

        if (processor != null)
        {
            MessageProcessorChainBuilder chainBuilder = new SimpleMessageProcessorChainBuilder();
            try
            {
                return chainBuilder.chain(processor, watermarkSource).build();
            }
            catch (MuleException e)
            {
                return processor;
            }
        }
        return watermarkSource;
    }

    /**
     * Registers a {@link org.mule.transport.polling.watermark.WatermarkStorePipelineListener} as listener of the flowConstruct passed as parameter.
     *
     * @see WatermarkConfiguration#registerPipelineNotificationListener(org.mule.api.construct.FlowConstruct)
     */
    @Override
    public void registerPipelineNotificationListener(FlowConstruct flowConstruct)
    {
        try
        {
            WatermarkStorePipelineListener watermarkListener = new WatermarkStorePipelineListener(muleContext, objectStore, variable, updateExpression);
            watermarkListener.setFlowConstruct(flowConstruct);
            muleContext.registerListener(watermarkListener);
        }
        catch (NotificationException e)
        {
            logger.error("The watermark processor could not be registered, the watermark will not be updated at the end" +
                         "of the flow.");
        }
    }


    public void setVariable(String variable)
    {
        this.variable = variable;
    }

    public void setDefaultExpression(String defaultExpression)
    {
        this.defaultExpression = defaultExpression;
    }

    public void setUpdateExpression(String updateExpression)
    {
        this.updateExpression = updateExpression;
    }

    public void setObjectStore(ObjectStore objectStore)
    {
        this.objectStore = objectStore;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
