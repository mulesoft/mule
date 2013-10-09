/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
