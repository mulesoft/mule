
package org.mule.transport.sftp.notification;

import java.util.Date;

import org.mule.api.MuleMessage;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.EndpointMessageNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointMessageNotificationTestListener implements EndpointMessageNotificationListener
{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void onNotification(ServerNotification notification)
    {

        EndpointMessageNotification endpointNotification;
        if (notification instanceof EndpointMessageNotification)
        {
            endpointNotification = (EndpointMessageNotification) notification;
        }
        else
        {
            logger.debug(
                "*** EndpointMessageNotificationTestListener RECEIVED UNKNOWN NOTIFICATION OF TYPE {}",
                notification.getClass().getName());
            return;
        }

        MuleMessage message = (MuleMessage) endpointNotification.getSource();
        String msgType = message.getPayload().getClass().getName();
        String correlationId = (String) message.getProperty("MULE_CORRELATION_ID", "?");
        // String endpointUri =
        // endpointNotification.getEndpoint().getEndpointURI().toString();
        String endpointName = endpointNotification.getEndpoint();
        String action = notification.getActionName();
        String resourceId = notification.getResourceIdentifier();
        String timestamp = new Date(notification.getTimestamp()).toString();

        if (logger.isDebugEnabled())
        {
            logger.debug("OnNotification: " + notification.EVENT_NAME + "\nTimestamp=" + timestamp
                         + "\nMsgType=" + msgType + "\nAction=" + action + "\nResourceId=" + resourceId
                         + "\nEndpointName=" + endpointName +
                         // "\nEndpointUri=" + endpointUri +
                         "\nCorrelationId=" + correlationId + "");
        }
    }

}
