/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-10618")
public class CxfContentTypeNonBlockingTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://example.cxf.module.compatibility.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0>Hello</arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getConfigFile() {
    return "cxf-echo-service-conf-httpn-nb.xml";
  }

  @Test
  @Ignore("MULE-10618")
  public void testCxfService() throws Exception {
    HttpRequest httpRequest =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/hello")
            .setEntity(new ByteArrayHttpEntity(requestPayload.getBytes()))
            .setMethod(POST).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String contentType = httpResponse.getHeaderValueIgnoreCase(CONTENT_TYPE);
    assertTrue(contentType.contains("charset"));
  }

  @Test
  @Ignore("MULE-10618")
  public void testCxfClient() throws Exception {
    HttpRequest httpRequest =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/helloClient")
            .setEntity(new ByteArrayHttpEntity("hello".getBytes()))
            .setMethod(POST).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String contentType = httpResponse.getHeaderValueIgnoreCase(CONTENT_TYPE);
    assertTrue(contentType.contains("charset"));
    getSensingInstance("sensingRequestResponseProcessor").assertRequestResponseThreadsDifferent();
  }

  @Test
  @Ignore("MULE-10618")
  public void testCxfClientProxy() throws Exception {
    HttpRequest httpRequest =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/helloClientProxy")
            .setEntity(new ByteArrayHttpEntity("hello".getBytes()))
            .setMethod(POST).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String contentType = httpResponse.getHeaderValueIgnoreCase(CONTENT_TYPE);
    assertNotNull(contentType);
    assertTrue(contentType.contains("charset"));
    getSensingInstance("sensingRequestResponseProcessorProxy").assertRequestResponseThreadsDifferent();
  }

  private SensingNullRequestResponseMessageProcessor getSensingInstance(String instanceBeanName) {
    return ((SensingNullRequestResponseMessageProcessor) muleContext.getRegistry().lookupObject(instanceBeanName));
  }

}
