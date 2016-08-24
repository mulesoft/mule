/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.functional;


import static org.junit.Assert.assertEquals;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class CxfJaxWsServiceAndClientTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(HttpConstants.Methods.POST.name()).build();

  private static final String REQUEST_PAYLOAD =
      "<soap:Envelope \n" + "           xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
          + "           xmlns:svc=\"http://example.cxf.module.runtime.mule.org/\">\n" + "<soap:Body>\n" + "<svc:sayHi>\n"
          + "    <arg0>Test Message</arg0>\n" + "</svc:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  private static final String RESPONSE_PAYLOAD = "<soap:Envelope " + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body>" + "<ns2:sayHiResponse xmlns:ns2=\"http://example.cxf.module.runtime.mule.org/\">" + "<return>"
      + "Hello\u2297 Test Message" + "</return>" + "</ns2:sayHiResponse>" + "</soap:Body>" + "</soap:Envelope>";

  @Override
  protected String getConfigFile() {
    return "cxf-jaxws-service-and-client-config-httpn.xml";
  }

  @Test
  public void jaxWsClientReadsMuleMethodPropertySetByJaxWsService() throws Exception {
    String url = "http://localhost:" + port.getNumber() + "/hello";
    MuleClient client = muleContext.getClient();

    MuleMessage result = client.send(url, getTestMuleMessage(REQUEST_PAYLOAD), HTTP_REQUEST_OPTIONS).getRight();

    assertEquals(RESPONSE_PAYLOAD, getPayloadAsString(result));
  }
}
