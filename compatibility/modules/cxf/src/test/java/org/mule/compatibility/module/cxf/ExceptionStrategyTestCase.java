/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.service.http.api.HttpConstants.RequestProperties.HTTP_STATUS_PROPERTY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExceptionStrategyTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://example.cxf.module.compatibility.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0>Hello</arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  private static final String requestFaultPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://cxf.module.compatibility.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0>Hello</arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  private CountDownLatch latch;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "exception-strategy-flow-conf-httpn.xml";
  }

  @Test
  public void testFaultInCxfService() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestFaultPayload).build();
    MuleClient client = muleContext.getClient();
    latch = new CountDownLatch(1);
    registerExceptionNotificationListener();
    InternalMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFault", request, HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains("<faultstring>"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
    assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testExceptionInCxfService() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestPayload).build();
    MuleClient client = muleContext.getClient();
    latch = new CountDownLatch(1);
    registerExceptionNotificationListener();
    InternalMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithException", request, HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains("<faultstring>"));
    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
    assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testClientWithTransformerExceptionDefaultException() throws Exception {
    expectedException.expect(MessagingException.class);
    expectedException.expectMessage("Failed to build message");
    flowRunner("FlowWithClientAndTransformerExceptionDefaultException").withPayload("hello").run();
  }

  @Test
  public void testClientWithFaultDefaultException() throws Exception {
    expectedException.expectCause(hasCause(instanceOf(Fault.class)));
    expectedException.expectMessage("Failed to route event");
    flowRunner("FlowWithClientWithFaultDefaultException").withPayload("hello").run();
  }

  @Test
  public void testServerClientProxyDefaultException() throws Exception {
    MuleClient client = muleContext.getClient();
    latch = new CountDownLatch(1);
    registerExceptionNotificationListener();
    InternalMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/proxyExceptionStrategy",
                                           InternalMessage.of(requestPayload), HTTP_REQUEST_OPTIONS)
        .getRight();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains("<faultstring>"));

    assertEquals(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()),
                 response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
    assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
  }

  protected void registerExceptionNotificationListener() throws NotificationException {
    // Do not inline this variable, otherwise the type of the listener is lost.
    ExceptionNotificationListener listener = notification -> latch.countDown();
    muleContext.registerListener(listener);
  }

  public static class CxfTransformerThrowsExceptions extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      throw new TransformerException(CoreMessages.failedToBuildMessage());
    }
  }

  public static class CustomProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      return event;
    }
  }
}
