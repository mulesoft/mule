/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.nio.charset.Charset;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CxfErrorBehaviorTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String requestFaultPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://cxf.module.compatibility.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0></arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  private static final String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://example.cxf.module.compatibility.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0>hi</arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";


  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  public static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  @Override
  protected String getConfigFile() {
    return "cxf-error-behavior-flow-httpn.xml";
  }

  @Test
  public void testFaultInCxfService() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestFaultPayload).build();
    MuleClient client = muleContext.getClient();
    InternalMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFault", request, HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains("<faultstring>"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
  }

  @Test
  public void testFaultInCxfSimpleService() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestPayload).build();
    MuleClient client = muleContext.getClient();
    InternalMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testSimpleServiceWithFault", request, HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains("<faultstring>"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
  }

  @Test
  public void testExceptionThrownInTransformer() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestPayload).build();
    MuleClient client = muleContext.getClient();
    InternalMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testTransformerException", request, HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains("<faultstring>"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
  }

  @Test
  public void testUnwrapException() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestPayload).build();
    MuleClient client = muleContext.getClient();
    InternalMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testUnwrapException", request, HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains("Illegal argument!!"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
  }

  @Test
  public void testClientWithSOAPFault() throws Exception {
    expectedException.expectCause(hasCause(instanceOf(Fault.class)));
    flowRunner("FlowWithClientAndSOAPFault").withPayload("hello").run().getMessage();
  }

  @Test
  public void testClientWithTransformerException() throws Exception {
    expectedException.expect(MessagingException.class);
    flowRunner("FlowWithClientAndTransformerException").withPayload("hello").run();
  }

  @Test
  public void testServerClientProxyWithFault() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithFault",
                                         InternalMessage.of(requestFaultPayload), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertThat(resString, containsString("<faultstring>Cxf Exception Message</faultstring>"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 result.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
  }

  @Test
  public void testServerClientProxyWithTransformerException() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithTransformerException",
                                         InternalMessage.of(requestPayload), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertTrue(resString.contains("TransformerException"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 result.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
  }

  @Test
  public void testServerClientJaxwsWithUnwrapFault() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testUnwrapProxyFault",
                                         InternalMessage.of(requestPayload), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertThat(resString, containsString("Illegal argument!!"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 result.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
  }

  public static class CxfTransformerThrowsExceptions extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      throw new TransformerException(CoreMessages.failedToBuildMessage());
    }
  }
}
