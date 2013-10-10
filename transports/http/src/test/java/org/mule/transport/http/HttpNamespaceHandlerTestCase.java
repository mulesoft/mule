/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.transport.http.filters.HttpRequestWildcardFilter;
import org.mule.transport.http.transformers.HttpClientMethodResponseToObject;
import org.mule.transport.http.transformers.HttpResponseToString;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class HttpNamespaceHandlerTestCase extends AbstractNamespaceHandlerTestCase
{

    public HttpNamespaceHandlerTestCase()
    {
        super("http");
    }

    @Test
    public void testConnectorProperties()
    {
        HttpConnector connector =
                (HttpConnector) muleContext.getRegistry().lookupConnector("httpConnector");
        testBasicProperties(connector);
    }

    @Test
    public void testPollingProperties()
    {
         HttpPollingConnector connector =
                (HttpPollingConnector) muleContext.getRegistry().lookupConnector("polling");
        assertNotNull(connector);
        assertEquals(3456, connector.getPollingFrequency());
        assertFalse(connector.isCheckEtag());
        assertFalse(connector.isDiscardEmptyContent());
    }
    
    @Test
    public void testTransformersOnEndpoints() throws Exception
    {
        Object transformer1 = lookupInboundEndpoint("ep1").getTransformers().get(0);
        assertNotNull(transformer1);
        assertEquals(HttpClientMethodResponseToObject.class, transformer1.getClass());

        Object transformer2 = lookupInboundEndpoint("ep2").getTransformers().get(0);
        assertNotNull(transformer2);
        assertEquals(HttpResponseToString.class, transformer2.getClass());
        
        Object transformer3 = lookupInboundEndpoint("ep3").getTransformers().get(0);
        assertNotNull(transformer3);
        assertEquals(MuleMessageToHttpResponse.class, transformer3.getClass());
        
        Object transformer4 = lookupInboundEndpoint("ep4").getTransformers().get(0);
        assertNotNull(transformer4);
        assertEquals(ObjectToHttpClientMethodRequest.class, transformer4.getClass());
    }

    @Test
    public void testFiltersOnEndpoints() throws Exception
    {
        Filter filter = lookupInboundEndpoint("ep5").getFilter();
        assertNotNull(filter);
        assertEquals(HttpRequestWildcardFilter.class, filter.getClass());
        HttpRequestWildcardFilter requestWildcardFilter = (HttpRequestWildcardFilter) filter;
        assertEquals("foo*", requestWildcardFilter.getPattern());
    }
    
    private InboundEndpoint lookupInboundEndpoint(String endpointName) throws Exception
    {
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(endpointName);
        assertNotNull(endpoint);
        return endpoint;
    }
}
