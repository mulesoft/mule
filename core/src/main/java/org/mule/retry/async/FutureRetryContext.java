/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
