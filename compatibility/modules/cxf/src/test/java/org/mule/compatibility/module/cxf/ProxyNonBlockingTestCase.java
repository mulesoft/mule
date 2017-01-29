/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-10618")
public class ProxyNonBlockingTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String ECHO_SOAP_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body><test xmlns=\"http://foo\"> foo </test></soap:Body>" + "</soap:Envelope>";

  private static final String GREETER_SOAP_TEST_ELEMENT_REQUEST =
      "<greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>";
  private static final String GREETER_SOAP_TEST_ELEMENT_RESPONSE =
      "<greetMeResponse xmlns=\"http://apache.org/hello_world_soap_http/types\"><responseType>Hello Dan</responseType></greetMeResponse>";

  private static final String GREETER_SOAP_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body>" + GREETER_SOAP_TEST_ELEMENT_REQUEST + "</soap:Body>" + "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getConfigFile() {
    return "proxy-conf-flow-httpn-nb.xml";
  }

  @Test
  @Ignore("MULE-10618")
  public void testEchoService() throws Exception {
    doTest("/services/echo", ECHO_SOAP_REQUEST, ECHO_SOAP_REQUEST);
  }

  @Test
  @Ignore("MULE-10618")
  public void testEchoProxy() throws Exception {
    doTest("/proxies/echo", ECHO_SOAP_REQUEST, ECHO_SOAP_REQUEST);
    getSensingInstance("sensingRequestResponseProcessorEcho").assertRequestResponseThreadsDifferent();
  }

  @Test
  @Ignore("MULE-10618")
  public void testGreeterService() throws Exception {
    doTest("/services/greeter", GREETER_SOAP_REQUEST, GREETER_SOAP_TEST_ELEMENT_RESPONSE);
  }

  @Test
  @Ignore("MULE-10618")
  public void testGreeterProxy() throws Exception {
    doTest("/proxies/greeter", GREETER_SOAP_REQUEST, GREETER_SOAP_TEST_ELEMENT_RESPONSE);
    getSensingInstance("sensingRequestResponseProcessorGreeter").assertRequestResponseThreadsDifferent();
  }

  private void doTest(String path, String requestPayload, String expectedResponse) throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + path)
            .setMethod(POST.name())
            .setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertThat(payload, containsString(expectedResponse));
  }

  private SensingNullRequestResponseMessageProcessor getSensingInstance(String instanceBeanName) {
    return ((SensingNullRequestResponseMessageProcessor) muleContext.getRegistry().lookupObject(instanceBeanName));
  }

}
