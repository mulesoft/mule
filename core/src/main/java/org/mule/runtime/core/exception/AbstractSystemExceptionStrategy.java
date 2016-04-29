/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transport.AbstractConnector;

import javax.resource.spi.work.Work;

/**
 * Fire a notification, log exception, clean up transaction if any, and trigger reconnection strategy 
 * if this is a <code>ConnectException</code>.
 */
public class AbstractSystemExceptionStrategy extends AbstractExceptionListener implements SystemExceptionHandler
{
    public AbstractSystemExceptionStrategy(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public void handleException(Exception ex, RollbackSourceCallback rollbackMethod)
    {
        fireNotification(ex);

        doLogException(ex);
        
        if (isRollback(ex))
        {
            logger.debug("Rolling back transaction");
            rollback(ex, rollbackMethod);
        }
        else
        {
            logger.debug("Committing transaction");
            commit();
        }

        ExceptionPayload exceptionPayload = new DefaultExceptionPayload(ex);
        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(exceptionPayload);
        }
        
        if (ex instanceof ConnectException)
        {
            handleReconnection((ConnectException) ex);
        }
    }

    private void rollback(Exception ex, RollbackSourceCallback rollbackMethod)
    {
        if (TransactionCoordination.getInstance().getTransaction() != null)
        {
            rollback(ex);
        }
        if (rollbackMethod != null)
        {
            rollbackMethod.rollback();
        }
    }

    @Override
    public void handleException(Exception ex)
    {
        handleException(ex, null);
    }
    
    protected void handleReconnection(ConnectException ex)
    {
        final AbstractConnector connector = (AbstractConnector) ex.getFailed();

        // Make sure the connector is not already being reconnected by another receiver thread.
        if (connector.isConnecting())
        {
            return;
        }

        logger.info("Exception caught is a ConnectException, attempting to reconnect...");

        // Disconnect
        try
        {
            logger.debug("Disconnecting " + connector.getName());
            connector.stop();
            connector.disconnect();
        }
        catch (Exception e1)
        {
            logger.error(e1.getMessage());
        }

        // Reconnect (retry policy will go into effect here if configured)
        try
        {
            connector.getMuleContext().getWorkManager().scheduleWork(new Work()
            {
                @Override
                public void release()
                {
                }

                @Override
                public void run()
                {
                    try
                    {
                        logger.debug("Reconnecting " + connector.getName());
                        connector.start();
                    }
                    catch (Exception e)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Error reconnecting", e);
                        }
                        logger.error(e.getMessage());
                    }
                }
            });
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Error executing reconnect work", e);
            }
            logger.error(e.getMessage());
        }
        //TODO See MULE-9307 - read reconnection behaviour for configs and sources
    }
}


