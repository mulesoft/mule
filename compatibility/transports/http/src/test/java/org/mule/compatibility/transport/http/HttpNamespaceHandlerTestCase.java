/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.compatibility.transport.http.HttpPollingConnector;
import org.mule.compatibility.transport.http.filters.HttpRequestWildcardFilter;
import org.mule.compatibility.transport.http.transformers.HttpClientMethodResponseToObject;
import org.mule.compatibility.transport.http.transformers.HttpResponseToString;
import org.mule.compatibility.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.compatibility.transport.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.runtime.core.api.routing.filter.Filter;

import org.junit.Test;

public class HttpNamespaceHandlerTestCase extends AbstractNamespaceHandlerTestCase {

  public HttpNamespaceHandlerTestCase() {
    super("http");
  }

  @Test
  public void testConnectorProperties() {
    HttpConnector connector = (HttpConnector) muleContext.getRegistry().lookupObject("httpConnector");
    testBasicProperties(connector);
  }

  @Test
  public void testPollingProperties() {
    HttpPollingConnector connector = (HttpPollingConnector) muleContext.getRegistry().lookupObject("polling");
    assertNotNull(connector);
    assertEquals(3456, connector.getPollingFrequency());
    assertFalse(connector.isCheckEtag());
    assertFalse(connector.isDiscardEmptyContent());
  }

  @Test
  public void testTransformersOnEndpoints() throws Exception {
    Object transformer1 = lookupInboundEndpoint("ep1").getMessageProcessors().get(0);
    assertNotNull(transformer1);
    assertEquals(HttpClientMethodResponseToObject.class, transformer1.getClass());

    Object transformer2 = lookupInboundEndpoint("ep2").getMessageProcessors().get(0);
    assertNotNull(transformer2);
    assertEquals(HttpResponseToString.class, transformer2.getClass());

    Object transformer3 = lookupInboundEndpoint("ep3").getMessageProcessors().get(0);
    assertNotNull(transformer3);
    assertEquals(MuleMessageToHttpResponse.class, transformer3.getClass());

    Object transformer4 = lookupInboundEndpoint("ep4").getMessageProcessors().get(0);
    assertNotNull(transformer4);
    assertEquals(ObjectToHttpClientMethodRequest.class, transformer4.getClass());
  }

  @Test
  public void testFiltersOnEndpoints() throws Exception {
    Filter filter = lookupInboundEndpoint("ep5").getFilter();
    assertNotNull(filter);
    assertEquals(HttpRequestWildcardFilter.class, filter.getClass());
    HttpRequestWildcardFilter requestWildcardFilter = (HttpRequestWildcardFilter) filter;
    assertEquals("foo*", requestWildcardFilter.getPattern());
  }

  private InboundEndpoint lookupInboundEndpoint(String endpointName) throws Exception {
    InboundEndpoint endpoint = getEndpointFactory().getInboundEndpoint(endpointName);
    assertNotNull(endpoint);
    return endpoint;
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
