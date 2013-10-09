/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpMethod;

public class MuleHttpMethodRetryHandler extends DefaultHttpMethodRetryHandler
{
    @Override
    public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount)
    {
        if ((executionCount < this.getRetryCount()) && (exception instanceof SocketException))
        {
            return true;
        }
        
        return super.retryMethod(method, exception, executionCount);
    }
}
