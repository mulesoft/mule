/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ProxyNonBlockingTestCase extends FunctionalTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

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

  @Override
  protected String getConfigFile() {
    return "proxy-conf-flow-httpn-nb.xml";
  }

  @Test
  public void testEchoService() throws Exception {
    doTest("/services/echo", ECHO_SOAP_REQUEST, ECHO_SOAP_REQUEST);
  }

  @Test
  public void testEchoProxy() throws Exception {
    doTest("/proxies/echo", ECHO_SOAP_REQUEST, ECHO_SOAP_REQUEST);
    getSensingInstance("sensingRequestResponseProcessorEcho").assertRequestResponseThreadsDifferent();
  }

  @Test
  public void testGreeterService() throws Exception {
    doTest("/services/greeter", GREETER_SOAP_REQUEST, GREETER_SOAP_TEST_ELEMENT_RESPONSE);
  }

  @Test
  public void testGreeterProxy() throws Exception {
    doTest("/proxies/greeter", GREETER_SOAP_REQUEST, GREETER_SOAP_TEST_ELEMENT_RESPONSE);
    getSensingInstance("sensingRequestResponseProcessorGreeter").assertRequestResponseThreadsDifferent();
  }

  private void doTest(String path, String request, String expectedResponse) throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result =
        client.send("http://localhost:" + dynamicPort.getNumber() + path, getTestMuleMessage(request), HTTP_REQUEST_OPTIONS)
            .getRight();
    String resString = getPayloadAsString(result);
    assertThat(resString, containsString(expectedResponse));
  }

  private SensingNullRequestResponseMessageProcessor getSensingInstance(String instanceBeanName) {
    return ((SensingNullRequestResponseMessageProcessor) muleContext.getRegistry().lookupObject(instanceBeanName));
  }

}
