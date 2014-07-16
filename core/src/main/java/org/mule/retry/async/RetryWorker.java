/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.retry.async;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.util.concurrent.Latch;

/**
 * A {@link javax.resource.spi.work.Work} implementation used when executing a {@link RetryPolicyTemplate} in a separate
 * thread.
 */
public class RetryWorker implements Work
{
    protected transient final Log logger = LogFactory.getLog(RetryWorker.class);

    private final RetryCallback callback;
    private final WorkManager workManager;
    private Exception exception = null;
    private final FutureRetryContext context = new FutureRetryContext();
    private final RetryPolicyTemplate delegate;
    private Latch startLatch;

    public RetryWorker(RetryPolicyTemplate delegate, RetryCallback callback, WorkManager workManager)
    {
        this(delegate, callback, workManager, null);
    }

    public RetryWorker(RetryPolicyTemplate delegate,
                       RetryCallback callback,
                       WorkManager workManager,
                       Latch startLatch)
    {
        this.callback = callback;
        this.workManager = workManager;
        this.delegate = delegate;
        this.startLatch = startLatch;
        if (this.startLatch == null)
        {
            this.startLatch = new Latch();
            this.startLatch.countDown();
        }
    }

    public void release()
    {

    }

    public void run()
    {
        try
        {
            startLatch.await();
        }
        catch (InterruptedException e)
        {
            logger.warn("Retry thread interupted for callback: " + callback.getWorkDescription());
            return;
        }
        try
        {
            context.setDelegateContext(delegate.execute(callback, workManager));
        }
        catch (Exception e)
        {
            this.exception = e;
            logger.fatal(e, e);

        }
    }

    public Exception getException()
    {
        return exception;
    }

    public FutureRetryContext getRetryContext()
    {
        return context;
    }
}
