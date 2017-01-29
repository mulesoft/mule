/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.functional;

import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.compatibility.module.cxf.example.HelloWorld;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.jws.WebService;

import org.junit.Rule;
import org.junit.Test;

public class UnwrapsComponentExceptionTestCase extends AbstractCxfOverHttpExtensionTestCase {

  public static final String ERROR_MESSAGE = "Changos!!!";

  private static final String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://example.cxf.module.compatibility.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0>Hello</arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getConfigFile() {
    return "unwraps-component-exception-config-httpn.xml";
  }

  @Test
  public void testReceivesComponentExceptionMessage() throws Exception {
    HttpRequest httpRequest = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/hello")
        .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertTrue("Component exception was not managed", payload.contains(ERROR_MESSAGE));
  }

  @WebService(endpointInterface = "org.mule.compatibility.module.cxf.example.HelloWorld", serviceName = "HelloWorld")
  public static class HelloWorldImpl implements HelloWorld {

    @Override
    public String sayHi(String text) {
      throw new RuntimeException(ERROR_MESSAGE);
    }
  }
}
