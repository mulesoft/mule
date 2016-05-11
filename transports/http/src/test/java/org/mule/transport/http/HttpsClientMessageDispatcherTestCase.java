/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URI;

import org.apache.commons.httpclient.HostConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class HttpsClientMessageDispatcherTestCase extends AbstractMuleTestCase
{

    @Test
    public void getHost() throws Exception
    {
        OutboundEndpoint oe = Mockito.mock(OutboundEndpoint.class);
        Connector connector = Mockito.mock(HttpsConnector.class);
        Mockito.when(oe.getConnector()).thenReturn(connector);
        HttpsClientMessageDispatcher dispatcher = new HttpsClientMessageDispatcher(oe);

        URI uri = new URI("https://www.mulesoft.org/");
        HostConfiguration hc1 = dispatcher.getHostConfig(uri);
        HostConfiguration hc2 = dispatcher.getHostConfig(uri);
        Assert.assertEquals(hc1, hc2);
    }
}
