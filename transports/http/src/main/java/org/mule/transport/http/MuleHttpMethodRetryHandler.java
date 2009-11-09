/*
 * $Id:MuleHttpMethodRetryHandler.java 7555 2007-07-18 03:17:16Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
