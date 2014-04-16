/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.config.ExceptionHelper;
import org.mule.transaction.TransactionCoordination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
* TODO: Find a better name for this class.
 */
public class MessagingExceptionGenerator
{
    protected transient Log logger = LogFactory.getLog(getClass());

    public MessagingException generateMessagingExceptionAndRollbackTransaction(Exception e, MuleEvent event)
    {
        MessagingException messagingException;
        if (e instanceof MessagingException)
        {
            messagingException = (MessagingException) e;
        }
        else
        {
            messagingException = new MessagingException(event, e);
        }
        try
        {
            logException(e);
            TransactionCoordination.getInstance().rollbackCurrentTransaction();
        }
        catch (Exception ex)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Failed to rollback transaction.", ex);
            }
        }
        return messagingException;
    }

    private void logException(Exception e)
    {
        logger.error("Exception during exception strategy execution");
        MuleException muleException = ExceptionHelper.getRootMuleException(e);
        if (muleException != null)
        {
            logger.error(muleException.getDetailedMessage());
        }
        else
        {
            logger.error("Caught exception in Exception Strategy: " + e.getMessage(), e);
        }
    }

}
