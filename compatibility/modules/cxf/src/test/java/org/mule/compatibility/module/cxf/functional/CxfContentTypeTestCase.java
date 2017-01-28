/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class CxfContentTypeTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://example.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n" + "    <arg0>Hello</arg0>\n"
      + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Override
  protected String getConfigFile() {
    return "cxf-echo-service-conf-httpn.xml";
  }

  @Test
  public void testCxfService() throws Exception {
    HttpRequest httpRequest =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/hello")
            .setEntity(new ByteArrayHttpEntity(requestPayload.getBytes()))
            .setMethod(POST.name()).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String contentType = httpResponse.getHeaderValueIgnoreCase(CONTENT_TYPE);
    assertTrue(contentType.contains("charset"));
  }

  @Test
  public void testCxfClient() throws Exception {
    InternalMessage received = flowRunner("helloServiceClient").withPayload("hello").run().getMessage();
    String contentType = received.getOutboundProperty("contentType");
    assertNotNull(contentType);
    assertTrue(contentType.contains("charset"));
  }

}
