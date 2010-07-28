/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.routing.reply;

import org.mule.routing.AbstractAggregator;

/**
 * TODO
 */
public abstract class AbstractResponseCallbackAggregator extends AbstractAggregator
{
    private String callbackMethod;

    public String getCallbackMethod()
    {
        return callbackMethod;
    }

    public void setCallbackMethod(String callbackMethod)
    {
        this.callbackMethod = callbackMethod;
    }
}