/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
