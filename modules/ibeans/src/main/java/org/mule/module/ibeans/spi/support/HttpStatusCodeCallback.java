/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi.support;

import org.mule.message.DefaultExceptionPayload;
import org.mule.module.ibeans.spi.MuleResponseMessage;

import org.ibeans.impl.test.MockMessageCallback;

/**
 * Sets a Http status code on the result message created on a mock invocation
 */
public class HttpStatusCodeCallback implements MockMessageCallback<MuleResponseMessage>
{
    private int status;

    public HttpStatusCodeCallback(int status)
    {
        this.status = status;
    }

    public void onMessage(MuleResponseMessage response)
    {
        //TODO should this really be read/write
        response.setStatusCode(String.valueOf(status));
        if(status >= 400)
        {
            response.getMessage().setExceptionPayload(new DefaultExceptionPayload(new Exception("Mock Http Error")));
        }
    }
}
