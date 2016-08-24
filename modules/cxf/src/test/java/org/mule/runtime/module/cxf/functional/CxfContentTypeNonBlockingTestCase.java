/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class CxfContentTypeNonBlockingTestCase extends FunctionalTestCase {

  private static final String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://example.cxf.module.runtime.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0>Hello</arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "cxf-echo-service-conf-httpn-nb.xml";
  }

  @Test
  public void testCxfService() throws Exception {
    MuleMessage request = MuleMessage.builder().payload(requestPayload).build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/hello", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    String contentType = received.getDataType().getMediaType().toRfcString();
    assertNotNull(contentType);
    assertTrue(contentType.contains("charset"));
  }

  @Test
  public void testCxfClient() throws Exception {
    MuleMessage request = MuleMessage.builder().payload("hello").build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/helloClient", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    String contentType = received.getInboundProperty("contentType");
    assertNotNull(contentType);
    assertTrue(contentType.contains("charset"));
    getSensingInstance("sensingRequestResponseProcessor").assertRequestResponseThreadsDifferent();
  }

  @Test
  public void testCxfClientProxy() throws Exception {
    MuleMessage request = MuleMessage.builder().payload("hello").build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/helloClientProxy", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    String contentType = received.getInboundProperty("contentType");
    assertNotNull(contentType);
    assertTrue(contentType.contains("charset"));
    getSensingInstance("sensingRequestResponseProcessorProxy").assertRequestResponseThreadsDifferent();
  }

  private SensingNullRequestResponseMessageProcessor getSensingInstance(String instanceBeanName) {
    return ((SensingNullRequestResponseMessageProcessor) muleContext.getRegistry().lookupObject(instanceBeanName));
  }

}
