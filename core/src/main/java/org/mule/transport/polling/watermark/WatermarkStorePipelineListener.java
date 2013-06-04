package org.mule.transport.polling.watermark;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.notification.PipelineMessageNotificationListener;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.context.notification.CustomMetadataNotification;
import org.mule.context.notification.PipelineMessageNotification;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link PipelineMessageNotificationListener} that acts only for a particular {@link FlowConstruct} on the action
 * {@link PipelineMessageNotification#PROCESS_END}. This listener stores the watermark value in the object store
 * with the watermark variable.
 *
 * It also implements the {@link MessageProcessor} interface as we need a message processor to fire
 * a {@link CustomMetadataNotification}. Also we open the possibility to use this class as part of a processor chain
 */
public class WatermarkStorePipelineListener extends WatermarkAction
        implements PipelineMessageNotificationListener<PipelineMessageNotification>, FlowConstructAware, MessageProcessor
{

    /**
     * Logger to notify errors.
     */
    private static Log logger = LogFactory.getLog(WatermarkStorePipelineListener.class);

    /**
     * The expression used to update the watermark value. If not present it uses flow variable value to update the
     * object store
     */
    private String updateExpression;

    /**
     * The flow construct that is listening
     */
    private FlowConstruct flowConstruct;

    public WatermarkStorePipelineListener(MuleContext muleContext,
                                          ObjectStore objectStore,
                                          String variable,
                                          String updateExpression)
    {
        super(muleContext, objectStore, variable);
        this.updateExpression = updateExpression;
    }

    @Override
    public void onNotification(PipelineMessageNotification notification)
    {
        try
        {
            MuleEvent muleEvent = (MuleEvent) notification.getSource();
            if (flowConstruct != null && flowConstruct.equals(muleEvent.getFlowConstruct())
                && notification.getAction() == PipelineMessageNotification.PROCESS_END)
            {
                this.process(muleEvent);
            }
        }
        catch (ObjectStoreException e)
        {
            logger.error("Could not store the watermark", e);
        }
        catch (MuleException e)
        {
            logger.error("Could not store the watermark", e);
        }

    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        String evaluatedVariable = evaluate(variable, event);
        Serializable watermarkValue = getWatermarkValue(event, evaluatedVariable);

        synchronized (objectStore)
        {

            if (objectStore.contains(evaluatedVariable))
            {
                objectStore.remove(evaluatedVariable);
            }
            if (watermarkValue != null)
            {
                objectStore.store(evaluatedVariable, watermarkValue);
            }
        }

        muleContext.fireNotification(new CustomMetadataNotification(event, this, WATERMARK_STORED_ATTRIBUTE_NAME,
                                                                    createMetadata(evaluatedVariable, watermarkValue)));
        return event;
    }


    private Serializable getWatermarkValue(MuleEvent event, String evaluatedVariable)
    {
        Serializable watermarkValue;
        if (updateExpression != null)
        {
            watermarkValue = (Serializable) getExpressionManager().evaluate(updateExpression, event);
        }
        else
        {
            watermarkValue = (Serializable) event.getMessage().getInvocationProperty(evaluatedVariable);
        }

        return watermarkValue;
    }

}
