/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class HttpJmsBridgeTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/sync/http-jms-bridge-flow.xml";
  }

  @Test
  public void testBridge() throws Exception {
    String payload = "payload";

    ParameterMap headersMap = new ParameterMap();
    final String customHeader = "X-Custom-Header";
    headersMap.put(customHeader, "value");

    HttpRequest request = HttpRequest.builder().setUri(format("http://localhost:%d/in", httpPort.getNumber()))
        .setEntity(new ByteArrayHttpEntity(payload.getBytes())).setHeaders(headersMap).setMethod(POST).build();
    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    Message msg = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(msg);
    assertThat(getPayloadAsString(msg), is(payload));
    assertThat(((InternalMessage) msg).getOutboundProperty(customHeader), is("value"));
  }
}
