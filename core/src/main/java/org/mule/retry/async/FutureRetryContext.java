/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.retry.async;

import java.util.Map;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.retry.RetryContext;

/**
 * An implementation of {@link RetryContext} to be used when a {@link org.mule.api.retry.RetryPolicyTemplate} is
 * executed in a separate thread via the {@link AsynchronousRetryTemplate}. A FutureRetryContext is a proxy to a real
 * {@link RetryContext} and provides access to the real context once it becomes available.
 */
public class FutureRetryContext implements RetryContext
{
    private RetryContext delegate;

    void setDelegateContext(RetryContext context)
    {
        this.delegate = context;
    }

    public boolean isReady()
    {
        return delegate != null;
    }

    protected void checkState()
    {
        if (!isReady())
        {
            throw new IllegalStateException(
                "Cannot perform operations on a FutureRetryContext until isReady() returns true");
        }
    }

    public void addReturnMessage(MuleMessage result)
    {
        checkState();
        delegate.addReturnMessage(result);
    }

    public String getDescription()
    {
        checkState();
        return delegate.getDescription();
    }

    public MuleMessage getFirstReturnMessage()
    {
        checkState();
        return delegate.getFirstReturnMessage();
    }

    public Map<Object, Object> getMetaInfo()
    {
        checkState();
        return delegate.getMetaInfo();
    }

    public MuleMessage[] getReturnMessages()
    {
        checkState();
        return delegate.getReturnMessages();
    }

    public void setReturnMessages(MuleMessage[] returnMessages)
    {
        checkState();
        delegate.setReturnMessages(returnMessages);
    }

    public Throwable getLastFailure()
    {
        checkState();
        return delegate.getLastFailure();
    }

    public void setOk()
    {
        checkState();
        delegate.setOk();
    }

    public void setFailed(Throwable lastFailure)
    {
        checkState();
        delegate.setFailed(lastFailure);
    }

    public boolean isOk()
    {
        checkState();
        return delegate.isOk();
    }

    public MuleContext getMuleContext()
    {
        checkState();
        return delegate.getMuleContext();
    }
}
