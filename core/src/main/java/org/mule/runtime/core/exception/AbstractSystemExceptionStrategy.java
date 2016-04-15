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
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.connector.ConnectException;

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

    public void handleException(Exception ex)
    {
        handleException(ex, null);
    }
    
    protected void handleReconnection(ConnectException ex)
    {
        //TODO See MULE-9307 - read reconnection behaviour for configs and sources
    }
}


