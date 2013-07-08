package org.mule.transport.polling.watermark;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.PipelineMessageNotificationListener;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ObjectStoreException;
import org.mule.context.notification.PipelineMessageNotification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link PipelineMessageNotificationListener} that acts only for a particular {@link FlowConstruct} on the action
 * {@link PipelineMessageNotification#PROCESS_END}. This listener stores the watermark value in the object store
 * with the watermark variable.
 * <p/>
 * It also implements the {@link MessageProcessor} interface as we need a message processor to fire
 * a {@link org.mule.context.notification.WatermarkNotification}. Also we open the possibility to use this class as part of a processor chain
 */
public class WatermarkPipelineListener implements PipelineMessageNotificationListener<PipelineMessageNotification>
{

    /**
     * Logger to notify errors.
     */
    private static Log logger = LogFactory.getLog(WatermarkPipelineListener.class);


    private Watermark watermark;
    /**
     * The flow construct that is listening
     */
    private FlowConstruct flowConstruct;


    public WatermarkPipelineListener(Watermark watermark, FlowConstruct flowConstruct)
    {
        this.watermark = watermark;
        this.flowConstruct = flowConstruct;
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
                watermark.store(muleEvent);
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



}
