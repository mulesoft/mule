/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.exception;

import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.transport.Connectable;
import org.mule.context.notification.ExceptionNotification;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.ConnectException;

import java.lang.reflect.InvocationTargetException;

/**
 * Log exception, fire a notification, and clean up transaction if any.
 */
public class DefaultSystemExceptionStrategy extends AbstractExceptionListener implements SystemExceptionHandler
{
    /** 
     * For IoC only 
     * @deprecated Use DefaultSystemExceptionStrategy(MuleContext muleContext) instead 
     */
    public DefaultSystemExceptionStrategy()
    {
        super();
    }
    
    public DefaultSystemExceptionStrategy(MuleContext muleContext)
    {
        super();
        setMuleContext(muleContext);
    }

    public void handleException(Exception e)
    {
        Connectable connectable = null;

        // unwrap any exception caused by using reflection apis, but only the top layer
        if (e instanceof InvocationTargetException)
        {
            Throwable t = e.getCause();
            // just because API accepts Exception, not Throwable :\
            e = t instanceof Exception ? (Exception) t : new Exception(t);
        }

        if (enableNotifications)
        {
            fireNotification(new ExceptionNotification(e));
        }

        if (e instanceof ConnectException)
        {
            logger.info("Exception caught is a ConnectException, attempting to reconnect...");
            connectable = ((ConnectException) e).getFailed();
            try
            {
                logger.debug("Disconnecting " + connectable.getClass().getName());
                connectable.disconnect();
            }
            catch (Exception e1)
            {
                logger.error(e1.getMessage());
            }
        }

        logException(e);
        
        handleTransaction(e);

        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(new DefaultExceptionPayload(e));
        }
        
        if (connectable != null)
        {
            // Reconnect (retry policy will go into effect here if configured)
            try
            {
                logger.debug("Reconnecting " + connectable.getClass().getName());
                connectable.connect();
            }
            catch (Exception e2)
            {
                logger.error(e2.getMessage());
            }
        }
    }
}
