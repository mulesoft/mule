package org.mule.transport.polling.watermark;


import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ObjectStore;
import org.mule.context.notification.CustomMetadataNotification;

import com.sun.tools.internal.ws.processor.modeler.ModelerConstants;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A Watermark message processor that will act as message source of the {@link org.mule.api.construct.FlowConstruct}
 */
public class WatermarkRetrieveMessageProcessor extends WatermarkAction implements MessageProcessor
{
    /**
     * The default expression to update the watermark variable
     *
     * @see org.mule.transport.polling.watermark.builder.DefaultWatermarkConfiguration#defaultExpression
     */
    private String defaultExpression;

    public WatermarkRetrieveMessageProcessor(MuleContext muleContext,
                                             ObjectStore objectStore,
                                             String variable,
                                             String defaultExpression)
    {
        super(muleContext, objectStore, variable);
        this.defaultExpression = defaultExpression;
    }

    /**
     * If the object store contains a value associated with the configured variable then it stores that value as a flow
     * variable in the {@link MuleEvent}, if there is no value associated then evaluates the defaultExpression and stores
     * that value as flow variable.
     *
     * @param event MuleEvent to be processed
     * @return The processed {@link MuleEvent}
     * @throws MuleException Does not throw any exception
     */
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        String evaluatedVariable = evaluate(variable, event);
        Serializable watermarkValue;
        if (objectStore.contains(evaluatedVariable))
        {
            watermarkValue = objectStore.retrieve(evaluatedVariable);
        }
        else
        {
            watermarkValue = evaluate(defaultExpression, event);
        }
        event.getMessage().setInvocationProperty(evaluatedVariable, watermarkValue);

        muleContext.fireNotification(new CustomMetadataNotification(event, this, WATERMARK_RETRIEVED_ACTION_NAME, createMetadata(evaluatedVariable, watermarkValue)));

        return event;
    }




}
