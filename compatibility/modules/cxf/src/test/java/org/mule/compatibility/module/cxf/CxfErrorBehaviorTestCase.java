/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
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
  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Override
  protected String getConfigFile() {
    return "cxf-error-behavior-flow-httpn.xml";
  }

  @Test
  public void testFaultInCxfService() throws Exception {
    HttpRequest request = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFault")
        .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(requestFaultPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(payload.contains("<faultstring>"));
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testFaultInCxfSimpleService() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/testSimpleServiceWithFault")
            .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(payload.contains("<faultstring>"));
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testExceptionThrownInTransformer() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/testTransformerException")
            .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(payload.contains("<faultstring>"));
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testUnwrapException() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/testUnwrapException")
            .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(payload.contains("Illegal argument!!"));
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
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
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithFault")
            .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(requestFaultPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(payload.contains("<faultstring>Cxf Exception Message</faultstring>"));
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testServerClientProxyWithTransformerException() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithTransformerException")
            .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(payload.contains("TransformerException"));
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testServerClientJaxwsWithUnwrapFault() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/testUnwrapProxyFault")
            .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(payload.contains("Illegal argument!!"));
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
  }

  public static class CxfTransformerThrowsExceptions extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      throw new TransformerException(CoreMessages.failedToBuildMessage());
    }
  }
}
