/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.retry;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.retry.RetryContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The RetryContext is used to store any data which carries over from
 * attempt to attempt such as response messages.
 */
public class DefaultRetryContext implements RetryContext, MuleContextAware
{
    private MuleMessage[] returnMessages;
    private Map<Object, Object> metaInfo = new HashMap<Object, Object>();
    private String description;
    private Throwable lastFailure;
    private boolean failed = false;
    private MuleContext muleContext;

    public DefaultRetryContext(String description, Map<Object, Object> metaInfo)
    {
        super();
        this.description = description;
        if (metaInfo != null)
        {
            this.metaInfo = metaInfo;
        }
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public Map<Object, Object> getMetaInfo()
    {
        return Collections.unmodifiableMap(metaInfo);
    }

    public MuleMessage[] getReturnMessages()
    {
        return returnMessages;
    }

    public MuleMessage getFirstReturnMessage()
    {
        return (returnMessages == null ? null : returnMessages[0]);
    }

    public void setReturnMessages(MuleMessage[] returnMessages)
    {
        this.returnMessages = returnMessages;
    }

    public void addReturnMessage(MuleMessage result)
    {
        if (returnMessages == null)
        {
            returnMessages = new MuleMessage[] {result};
        }
        else
        {
            MuleMessage[] newReturnMessages = new MuleMessage[returnMessages.length + 1];
            System.arraycopy(newReturnMessages, 0, returnMessages, 0, 1);
            returnMessages = newReturnMessages;
        }
    }

    public String getDescription()
    {
        return description;
    }

    public Throwable getLastFailure()
    {
        return this.lastFailure;
    }

    public void setOk()
    {
        this.failed = false;
        this.lastFailure = null;
    }

    public boolean isOk()
    {
        // note that it might be possible to fail without throwable, so not relying on lastFailure field
        return !this.failed;
    }

    public void setFailed(Throwable lastFailure)
    {
        this.failed = true;
        this.lastFailure = lastFailure;
    }
}
