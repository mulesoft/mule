/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;


import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.nio.charset.Charset;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;


public class OnErrorContinueTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String requestPayload =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
          "           xmlns:hi=\"http://example.cxf.module.compatibility.mule.org/\">\n" +
          "<soap:Body>\n" +
          "<hi:sayHi>\n" +
          "    <arg0>Hello</arg0>\n" +
          "</hi:sayHi>\n" +
          "</soap:Body>\n" +
          "</soap:Envelope>";

  private static final String requestFaultPayload =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
          "           xmlns:hi=\"http://cxf.module.compatibility.mule.org/\">\n" +
          "<soap:Body>\n" +
          "<hi:sayHi>\n" +
          "    <arg0>Hello</arg0>\n" +
          "</hi:sayHi>\n" +
          "</soap:Body>\n" +
          "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Override
  protected String getConfigFile() {
    return "on-error-continue-conf.xml";
  }

  @Test
  public void testFaultInCxfServiceWithCatchExceptionStrategy() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFaultCatchException")
            .setMethod(POST.name())
            .setEntity(new ByteArrayHttpEntity(requestFaultPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertEquals(OK.getStatusCode(), response.getStatusCode());
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains("Anonymous"));
  }

  @Test
  public void testFaultInCxfServiceWithCatchExceptionStrategyRethrown() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .setUri("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFaultCatchExceptionRethrown")
        .setMethod(POST.name())
        .setEntity(new ByteArrayHttpEntity(requestFaultPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains("<faultstring>"));
  }

  @Test
  public void testExceptionThrownInTransformerWithCatchExceptionStrategy() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .setUri("http://localhost:" + dynamicPort.getNumber() + "/testTransformerExceptionCatchException")
        .setMethod(POST.name())
        .setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertEquals(OK.getStatusCode(), response.getStatusCode());
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains("APPEND"));
  }

  @Test
  public void testClientWithSOAPFaultCatchException() throws Exception {
    Event event = flowRunner("FlowWithClientAndSOAPFaultCatchException").withPayload("hello").run();
    assertNotNull(event);
    assertThat(event.getError().isPresent(), is(false));
  }

  @Test
  public void testClientWithSOAPFaultCatchExceptionRedirect() throws Exception {
    Event event = flowRunner("FlowWithClientAndSOAPFaultCatchExceptionRedirect").withPayload("TEST").run();
    assertNotNull(event);
    assertNotNull(event.getMessage());
    assertThat(getPayloadAsString(event.getMessage()), containsString("TEST"));
    assertThat(event.getError().isPresent(), is(false));
  }

  @Test
  public void testClientWithTransformerExceptionCatchException() throws Exception {
    InternalMessage response =
        flowRunner("FlowWithClientAndTransformerExceptionCatchException").withPayload("hello").run().getMessage();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains(" Anonymous"));
  }

  @Test
  public void testServerClientProxyWithTransformerExceptionCatchStrategy() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .setUri("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithTransformerExceptionCatchStrategy")
        .setMethod(POST.name())
        .setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertEquals(OK.getStatusCode(), response.getStatusCode());
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains("Anonymous"));
  }

  public static class ProxyCustomProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      String payload =
          "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
              + "<soap:Body>"
              + "<ns2:sayHiResponse xmlns:ns2=\"http://example.cxf.module.compatibility.mule.org/\">"
              + "<return>Hello Anonymous</return>"
              + "</ns2:sayHiResponse>"
              + "</soap:Body>"
              + "</soap:Envelope>";
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(payload).build()).build();
    }
  }

  public static class RethrowFaultProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new Fault(event.getError().get().getCause().getCause());
    }
  }

  public static class RethrowExceptionStrategy extends TemplateOnErrorHandler {

    @Override
    protected Event nullifyExceptionPayloadIfRequired(Event event) {
      return event;
    }

    @Override
    protected Event afterRouting(MessagingException exception, Event event) {
      return event;
    }

    @Override
    protected Event beforeRouting(MessagingException exception, Event event) {
      return event;
    }
  }

  public static class CxfTransformerThrowsExceptions extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      throw new TransformerException(CoreMessages.failedToBuildMessage());
    }

  }

}
