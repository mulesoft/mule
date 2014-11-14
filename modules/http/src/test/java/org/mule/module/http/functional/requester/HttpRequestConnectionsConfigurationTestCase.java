/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.internal.request.HttpRequestConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class HttpRequestConnectionsConfigurationTestCase extends AbstractMuleTestCase
{

    @Test(expected=InitialisationException.class)
    public void invalidMaxConcurrentConnections() throws InitialisationException
    {
        HttpRequestConfig httpRequestConfig = new HttpRequestConfig();
        httpRequestConfig.setMaxConcurrentConnections(-2);
        httpRequestConfig.initialise();
    }

    @Test(expected=InitialisationException.class)
    public void invalidMaxConcurrentConnections0() throws InitialisationException
    {
        HttpRequestConfig httpRequestConfig = new HttpRequestConfig();
        httpRequestConfig.setMaxConcurrentConnections(0);
        httpRequestConfig.initialise();
    }

    @Test(expected=InitialisationException.class)
    public void nonPersistentConnectionsCanNotHaveIdleTimeout() throws InitialisationException
    {
        HttpRequestConfig httpRequestConfig = new HttpRequestConfig();
        httpRequestConfig.setUsePersistentConnections(false);
        httpRequestConfig.setConnectionIdleTimeout(100);
        httpRequestConfig.initialise();
    }

}
