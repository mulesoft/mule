/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.issues;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.cxf.CxfBasicTestCase.APP_SOAP_XML;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProxyMule6829TestCase extends FunctionalTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

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
      QName cxfOperation = context.getEvent().getFlowVariable("cxf_operation");
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
    MuleMessage response = executeSoap11Call(msgEchoOperation1, soapOperation);
    assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
    String cxfOperationName = testCxfEventCallback.getCxfOperationName();
    assertEquals(soapOperation, cxfOperationName);
    String payload = getPayloadAsString(response);
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
    MuleMessage response = executeSoap11Call(msgEchoOperation2, soapOperation);
    assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
    String cxfOperationName = testCxfEventCallback.getCxfOperationName();
    assertEquals(soapOperation, cxfOperationName);
    String payload = getPayloadAsString(response);
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
    MuleMessage response = executeSoap12Call(msgEchoOperation1, soapOperation);
    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    String cxfOperationName = testCxfEventCallback.getCxfOperationName();
    assertEquals(soapOperation, cxfOperationName);
    String payload = getPayloadAsString(response);
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
    MuleMessage response = executeSoap12Call(msgEchoOperation2, soapOperation);
    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    String cxfOperationName = testCxfEventCallback.getCxfOperationName();
    assertEquals(soapOperation, cxfOperationName);
    String payload = getPayloadAsString(response);
    assertTrue(payload.contains("<new:parameter2"));
    assertTrue(payload.contains("hello world"));
  }

  private MuleMessage executeSoap11Call(String msgString, String soapAction) throws MuleException {
    MuleMessage msg = MuleMessage.builder().payload(msgString).addOutboundProperty("soapAction", soapAction).build();

    return muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/EchoService11", msg,
                                        HTTP_REQUEST_OPTIONS)
        .getRight();
  }

  private MuleMessage executeSoap12Call(String msgString, String soapAction) throws MuleException {
    String contentType = APP_SOAP_XML.withCharset(UTF_8).toRfcString() + ";action=\"" + soapAction + "\"";
    MuleMessage msg = MuleMessage.builder().payload(msgString).mediaType(MediaType.parse(contentType)).build();

    return muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/EchoService12", msg,
                                        HTTP_REQUEST_OPTIONS)
        .getRight();
  }
}
