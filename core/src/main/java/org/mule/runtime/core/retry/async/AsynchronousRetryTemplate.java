/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.retry.async;

import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.api.retry.RetryPolicy;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.util.concurrent.Latch;

import java.util.Map;

import javax.resource.spi.work.WorkException;

/**
 * This class is a wrapper for a {@link RetryPolicyTemplate} and will execute any retry work within a separate thread.
 * An optional {@link Latch} can be passed into this template, in which case execution will only occur once the latch is
 * released.
 */
public class AsynchronousRetryTemplate implements RetryPolicyTemplate
{
    private final RetryPolicyTemplate delegate;
    private Latch startLatch;

    public AsynchronousRetryTemplate(RetryPolicyTemplate delegate)
    {
        this.delegate = delegate;
    }

    public RetryContext execute(RetryCallback callback, WorkManager workManager) throws Exception
    {
        if (workManager == null)
        {
            throw new IllegalStateException(
                "Cannot schedule a work till the workManager is initialized. Probably the connector hasn't been initialized yet");
        }

        RetryWorker worker = new RetryWorker(delegate, callback, workManager, startLatch);
        FutureRetryContext context = worker.getRetryContext();

        try
        {
            workManager.scheduleWork(worker);
        }
        catch (WorkException e)
        {
            throw new RetryPolicyExhaustedException(e, callback.getWorkOwner());
        }
        return context;
    }

    public RetryPolicy createRetryInstance()
    {
        return delegate.createRetryInstance();
    }

    public RetryNotifier getNotifier()
    {
        return delegate.getNotifier();
    }

    public void setNotifier(RetryNotifier retryNotifier)
    {
        delegate.setNotifier(retryNotifier);
    }

    public Map<Object, Object> getMetaInfo()
    {
        return delegate.getMetaInfo();
    }

    public void setMetaInfo(Map<Object, Object> metaInfo)
    {
        delegate.setMetaInfo(metaInfo);
    }

    public RetryPolicyTemplate getDelegate()
    {
        return delegate;
    }

    public void setStartLatch(Latch latch)
    {
        this.startLatch = latch;
    }
}
