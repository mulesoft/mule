/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.retry.notifiers;

import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.config.ExceptionHelper;
import org.mule.context.notification.ConnectionNotification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Fires a {@link ConnectionNotification} each time a retry attempt is made.
 */
public class ConnectNotifier implements RetryNotifier
{
    protected transient final Log logger = LogFactory.getLog(ConnectNotifier.class);

    public void onSuccess(RetryContext context)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Successfully connected to " + context.getDescription());
        }

        fireConnectNotification(ConnectionNotification.CONNECTION_CONNECTED, context.getDescription(), context);
    }

    public void onFailure(RetryContext context, Throwable e)
    {
        fireConnectNotification(ConnectionNotification.CONNECTION_FAILED, context.getDescription(), context);

        if (logger.isErrorEnabled())
        {
            StringBuffer msg = new StringBuffer(512);
            msg.append("Failed to connect/reconnect: ").append(context.getDescription());
            Throwable t = ExceptionHelper.getRootException(e);
            msg.append(". Root Exception was: ").append(ExceptionHelper.writeException(t));
            if (logger.isTraceEnabled())
            {
                t.printStackTrace();
            }
            logger.error(msg.toString());
        }
    }

    protected void fireConnectNotification(int action, String description, RetryContext context)
    {
        context.getMuleContext().fireNotification(new ConnectionNotification(null, description, action));
    }
}
