/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestWildcardFilterTestCase extends FunctionalTestCase {

  private static final String TEST_HTTP_MESSAGE = "Hello=World";
  private static final String TEST_BAD_MESSAGE = "xyz";

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "http-wildcard-filter-test-service.xml";
  }

  @Test
  public void testReference() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client
        .send(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("reference")).getMessageSource()).getAddress(),
              TEST_HTTP_MESSAGE, null);

    assertEquals(TEST_HTTP_MESSAGE, getPayloadAsString(result));
  }

  @Test
  public void testHttpPost() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result =
        client.send(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("httpIn")).getMessageSource()).getAddress(),
                    TEST_HTTP_MESSAGE, null);

    assertEquals(TEST_HTTP_MESSAGE, getPayloadAsString(result));
  }

  @Test
  public void testHttpGetNotFiltered() throws Exception {
    Map<String, Serializable> props = new HashMap<>();
    props.put(HttpConstants.METHOD_GET, "true");

    MuleClient client = muleContext.getClient();
    MuleMessage result =
        client.send(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("httpIn")).getMessageSource()).getAddress()
            + "/" + "mulerulez", TEST_HTTP_MESSAGE, props);

    assertEquals(TEST_HTTP_MESSAGE, getPayloadAsString(result));
  }

  @Test
  public void testHttpGetFiltered() throws Exception {
    Map<String, Serializable> props = new HashMap<>();
    props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
    // props.put(HttpConstants.METHOD_GET, "true");

    MuleClient client = muleContext.getClient();
    MuleMessage result =
        client.send(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("httpIn")).getMessageSource()).getAddress()
            + "/" + TEST_BAD_MESSAGE, "mule", props);

    final int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
    assertEquals(HttpConstants.SC_NOT_ACCEPTABLE, status);
    assertNotNull(result.getExceptionPayload());
  }
}
