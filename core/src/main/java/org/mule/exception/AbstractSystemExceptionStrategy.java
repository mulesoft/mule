/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.exception;

import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;

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

    public void handleException(Exception ex, RollbackSourceCallback rollbackMethod)
    {
        fireNotification(ex);

        logException(ex);
        
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

    public void handleException(Exception ex)
    {
        handleException(ex, null);
    }
    
    protected void handleReconnection(ConnectException ex)
    {
        AbstractConnector connector = (AbstractConnector) ex.getFailed();

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
            logger.debug("Reconnecting " + connector.getName());
            connector.connect();
            connector.start();
        }
        catch (Exception e2)
        {
            logger.error(e2.getMessage());
        }
    }
}


