/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpListenerExpectHeaderStreamingAlwaysTestCase extends HttpListenerExpectHeaderStreamingNeverTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-listener-expect-header-streaming-always-config.xml";
    }

    public HttpListenerExpectHeaderStreamingAlwaysTestCase(String persistentConnections)
    {
        super(persistentConnections);
    }

    @Override
    protected String getExpectedResponseBody()
    {
        return "c\r\n" + TEST_MESSAGE + "\r\n0\r\n\r";
    }
}

