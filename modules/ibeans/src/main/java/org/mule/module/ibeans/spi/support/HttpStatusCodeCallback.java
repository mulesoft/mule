/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
