/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.retry.policies;

import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.api.retry.RetryPolicy;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.DefaultRetryContext;
import org.mule.retry.PolicyStatus;
import org.mule.retry.notifiers.ConnectNotifier;

import java.io.InterruptedIOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for PolicyFactory implementations
 * */
public abstract class AbstractPolicyTemplate implements RetryPolicyTemplate
{
    /** should the retry template using this policy be executed in its own thread */
    protected boolean connectAsychronously = false;

    protected RetryNotifier notifier = new ConnectNotifier();
    
    protected transient final Log logger = LogFactory.getLog(getClass());
    
    public RetryContext execute(RetryCallback callback) throws Exception
    {
        PolicyStatus status = null;
        RetryPolicy policy = createRetryInstance();
        DefaultRetryContext context = new DefaultRetryContext(callback.getWorkDescription());
        try
        {
            Exception cause = null;
            do
            {
                try
                {
                    callback.doWork(context);
                    if (notifier != null)
                    {
                        notifier.sucess(context);
                    }
                    break;
                }
                catch (Exception e)
                {
                    cause = e;
                    if (notifier != null)
                    {
                        notifier.failed(context, cause);
                    }
                    if (cause instanceof InterruptedException || cause instanceof InterruptedIOException)
                    {
                        logger.error("Process was interrupted (InterruptedException), ceasing process");
                        break;
                    }
                    else
                    {
                        status = policy.applyPolicy(cause);
                    }
                }
            }
            while (status.isOk());

            if (status == null || status.isOk())
            {
                return context;
            }
            else
            {
                throw cause;
                // TODO Do we really want to wrap the exception here or just bubble up the original exception?
                //throw new FatalConnectException(
                //        CoreMessages.failedToConnect(context.getDescription(), this),
                //        status.getThrowable(), callback.getWorkDescription());
            }
        }
        finally
        {
            if (status != null && status.getThrowable() != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.error(status.getThrowable());
                }
            }
        }
    }
    
    /**
    * @return true if a policy is configured which will actually retry
    */
    public boolean isRetryEnabled()
    {
        return true;
    }

    /**
     * should the retry template using this policy be executed in its own thread
     * @return
     */
    public boolean isConnectAsynchronously()
    {
        return connectAsychronously;
    }

    /**
     * should the retry template using this policy be executed in its own thread
     * @param conectAsychronously
     */
    public void setConectAsychronously(boolean connectAsychronously)
    {
        this.connectAsychronously = connectAsychronously;
    }

    public RetryNotifier getNotifier()
    {
        return notifier;
    }

    public void setNotifier(RetryNotifier retryNotifier)
    {
        this.notifier = retryNotifier;
    }

    // For Spring IoC only
    public void setId(String id)
    {
        // ignore
    }
}
