/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.handlers;

import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;

public class TestRetryNotifier implements RetryNotifier
{
    private String color = "blue";
    
    public void onSuccess(RetryContext context)
    {
        // empty
    }

    public void onFailure(RetryContext context, Throwable e)
    {
        // empty
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }
}
