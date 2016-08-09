/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests as per http://www.io.com/~maus/HttpKeepAlive.html
 */
public class Http10FunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-10-config-flow.xml";
  }

  private HttpClient setupHttpClient() {
    HttpClientParams params = new HttpClientParams();
    params.setVersion(HttpVersion.HTTP_1_0);
    return new HttpClient(params);
  }

  @Test
  public void testHttp10EnforceNonChunking() throws Exception {
    HttpClient client = setupHttpClient();
    GetMethod request =
        new GetMethod(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("Streaming")).getMessageSource())
            .getAddress());
    client.executeMethod(request);
    assertEquals("hello", request.getResponseBodyAsString());

    assertNull(request.getResponseHeader(HttpConstants.HEADER_TRANSFER_ENCODING));
    assertNotNull(request.getResponseHeader(HttpConstants.HEADER_CONTENT_LENGTH));
  }
}
