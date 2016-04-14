/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.retry.policies;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.api.retry.RetryPolicy;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.DefaultRetryContext;
import org.mule.retry.PolicyStatus;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.retry.notifiers.ConnectNotifier;

import java.io.InterruptedIOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for RetryPolicyTemplate implementations.  Uses ConnectNotifier as RetryNotifier
 * by default.
 */
public abstract class AbstractPolicyTemplate implements RetryPolicyTemplate, MuleContextAware
{
    protected RetryNotifier notifier = new ConnectNotifier();
    
    /** This data will be made available to the RetryPolicy via the RetryContext. */
    private Map<Object, Object> metaInfo;

    private MuleContext muleContext;

    protected transient final Log logger = LogFactory.getLog(getClass());

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public RetryContext execute(RetryCallback callback, WorkManager workManager) throws Exception
    {
        PolicyStatus status = null;
        RetryPolicy policy = createRetryInstance();
        DefaultRetryContext context = new DefaultRetryContext(callback.getWorkDescription(), 
            metaInfo);
        context.setMuleContext(muleContext);

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
                        notifier.onSuccess(context);
                    }
                    break;
                }
                catch (Exception e)
                {
                    cause = e;
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(cause);
                    }
                    if (notifier != null)
                    {
                        notifier.onFailure(context, cause);
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
                context.setFailed(cause);
                throw new RetryPolicyExhaustedException(cause, callback.getWorkOwner());
            }
        }
        finally
        {
            if (status != null && status.getThrowable() != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(status.getThrowable());
                }
            }
        }
    }
    
    public RetryNotifier getNotifier()
    {
        return notifier;
    }

    public void setNotifier(RetryNotifier retryNotifier)
    {
        this.notifier = retryNotifier;
    }

    public Map<Object, Object> getMetaInfo()
    {
        return metaInfo;
    }

    public void setMetaInfo(Map<Object, Object> metaInfo)
    {
        this.metaInfo = metaInfo;
    }

    // For Spring IoC only
    public void setId(String id)
    {
        // ignore
    }
}
