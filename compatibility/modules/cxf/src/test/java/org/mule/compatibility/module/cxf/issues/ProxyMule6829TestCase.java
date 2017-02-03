/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.issues;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.compatibility.module.cxf.CxfBasicTestCase.APP_SOAP_XML;
import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpConstants.Method.POST;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProxyMule6829TestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  private Latch latch;
  private TestCxfEventCallback testCxfEventCallback;

  @Override
  protected String getConfigFile() {
    return "proxy-mule-6829-httpn.xml";
  }

  private static class TestCxfEventCallback implements EventCallback {

    private final Latch latch;
    private String cxfOperationName;

    private TestCxfEventCallback(Latch latch) {
      this.latch = latch;
    }

    @Override
    public void eventReceived(MuleEventContext context, Object component, MuleContext muleContext) throws Exception {
      QName cxfOperation = (QName) context.getEvent().getVariable("cxf_operation").getValue();
      cxfOperationName = cxfOperation.getLocalPart();

      latch.countDown();
    }

    public String getCxfOperationName() {
      return cxfOperationName;
    }
  }

  @Before
  public void before() {
    latch = new Latch();
    testCxfEventCallback = new TestCxfEventCallback(latch);
  }

  @Test
  public void testProxyServerSoap11Op1() throws Exception {
    FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("soap11Flow");
    testComponent.setEventCallback(testCxfEventCallback);

    String msgEchoOperation1 =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    String soapOperation = "EchoOperation1";
    HttpResponse httpResponse = executeSoap11Call(msgEchoOperation1, soapOperation);
    assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
    String cxfOperationName = testCxfEventCallback.getCxfOperationName();
    assertEquals(soapOperation, cxfOperationName);
    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertTrue(payload.contains("<new:parameter1"));
    assertTrue(payload.contains("hello world"));
  }

  @Test
  public void testProxyServerSoap11Op2() throws Exception {
    FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("soap11Flow");
    testComponent.setEventCallback(testCxfEventCallback);

    String msgEchoOperation2 =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter2>hello world</new:parameter2>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    String soapOperation = "EchoOperation2";
    HttpResponse httpResponse = executeSoap11Call(msgEchoOperation2, soapOperation);
    assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
    String cxfOperationName = testCxfEventCallback.getCxfOperationName();
    assertEquals(soapOperation, cxfOperationName);
    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertTrue(payload.contains("<new:parameter2"));
    assertTrue(payload.contains("hello world"));
  }

  @Test
  public void testProxyServerSoap12Op1() throws Exception {
    FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("soap12Flow");
    testComponent.setEventCallback(testCxfEventCallback);

    String msgEchoOperation1 =
        "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soap:Header/>" + "  <soap:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soap:Body>"
            + "</soap:Envelope>";

    String soapOperation = "EchoOperation1";
    HttpResponse httpResponse = executeSoap12Call(msgEchoOperation1, soapOperation);
    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    String cxfOperationName = testCxfEventCallback.getCxfOperationName();
    assertEquals(soapOperation, cxfOperationName);
    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertTrue(payload.contains("<new:parameter1"));
    assertTrue(payload.contains("hello world"));
  }

  @Test
  public void testProxyServerSoap12Op2() throws Exception {
    FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("soap12Flow");
    testComponent.setEventCallback(testCxfEventCallback);

    String msgEchoOperation2 =
        "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soap:Header/>" + "  <soap:Body>" + "    <new:parameter2>hello world</new:parameter2>" + "  </soap:Body>"
            + "</soap:Envelope>";

    String soapOperation = "EchoOperation2";
    HttpResponse httpResponse = executeSoap12Call(msgEchoOperation2, soapOperation);
    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    String cxfOperationName = testCxfEventCallback.getCxfOperationName();
    assertEquals(soapOperation, cxfOperationName);
    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertTrue(payload.contains("<new:parameter2"));
    assertTrue(payload.contains("hello world"));
  }

  private HttpResponse executeSoap11Call(String msgString, String soapAction)
      throws MuleException, IOException, TimeoutException {
    ParameterMap headersMap = new ParameterMap();
    headersMap.put("soapAction", soapAction);
    HttpRequest httpRequest = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/EchoService11")
        .setMethod(POST).setEntity(new ByteArrayHttpEntity(msgString.getBytes())).setHeaders(headersMap).build();

    return httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);
  }

  private HttpResponse executeSoap12Call(String msgString, String soapAction)
      throws MuleException, IOException, TimeoutException {
    String contentType = APP_SOAP_XML.withCharset(UTF_8).toRfcString() + ";action=\"" + soapAction + "\"";
    ParameterMap headersMap = new ParameterMap();
    headersMap.put(CONTENT_TYPE, contentType);
    HttpRequest httpRequest = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/EchoService12")
        .setMethod(POST).setEntity(new ByteArrayHttpEntity(msgString.getBytes())).setHeaders(headersMap).build();

    return httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);
  }
}
