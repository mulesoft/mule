/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.transport.http.transformers.HttpClientMethodResponseToObject;
import org.mule.transport.http.transformers.HttpResponseToString;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;


public class HttpNamespaceHandlerTestCase extends AbstractNamespaceHandlerTestCase
{

    public HttpNamespaceHandlerTestCase()
    {
        super("http");
    }

    public void testConnectorProperties()
    {
        HttpConnector connector =
                (HttpConnector) muleContext.getRegistry().lookupConnector("httpConnector");
        testBasicProperties(connector);
    }
    
    public void testTransformersOnEndpoints() throws Exception
    {
        Object transformer1 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ep1").getTransformers().get(0);
        assertNotNull(transformer1);
        assertEquals(HttpClientMethodResponseToObject.class, transformer1.getClass());

        Object transformer2 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ep2").getTransformers().get(0);
        assertNotNull(transformer2);
        assertEquals(HttpResponseToString.class, transformer2.getClass());
        
        Object transformer3 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ep3").getTransformers().get(0);
        assertNotNull(transformer3);
        assertEquals(MuleMessageToHttpResponse.class, transformer3.getClass());
        
        Object transformer4 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ep4").getTransformers().get(0);
        assertNotNull(transformer4);
        assertEquals(ObjectToHttpClientMethodRequest.class, transformer4.getClass());
    }

}
