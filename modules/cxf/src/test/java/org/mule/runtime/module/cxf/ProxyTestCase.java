/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeXml;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.ACCEPTED;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.cxf.testmodels.AsyncService;
import org.mule.runtime.module.cxf.testmodels.AsyncServiceWithSoapAction;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

public class ProxyTestCase extends FunctionalTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body><test xmlns=\"http://foo\"> foo </test>" + "</soap:Body>" + "</soap:Envelope>";

  String doGoogleSearch =
      "<urn:doGoogleSearch xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:urn=\"urn:GoogleSearch\">";

  String msgWithComment = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<!-- comment 1 -->"
      + "<soap:Header>" + "<!-- comment 2 -->" + "</soap:Header>" + "<!-- comment 3 -->" + "<soap:Body>" + "<!-- comment 4 -->"
      + doGoogleSearch + "<!-- this comment breaks it -->" + "<key>1</key>" + "<!-- comment 5 -->" + "<q>a</q>"
      + "<start>0</start>" + "<maxResults>1</maxResults>" + "<filter>false</filter>" + "<restrict>a</restrict>"
      + "<safeSearch>true</safeSearch>" + "<lr>a</lr>" + "<ie>b</ie>" + "<oe>c</oe>" + "</urn:doGoogleSearch>"
      + "<!-- comment 6 -->" + "</soap:Body>" + "<!-- comment 7 -->" + "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "proxy-conf-flow-httpn.xml";
  }

  @Test
  public void testServerWithEcho() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/Echo",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertTrue(resString.indexOf("<test xmlns=\"http://foo\"> foo </test>") != -1);
  }

  @Test
  public void testServerClientProxy() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <foo xmlns=\"http://foo\"></foo>" + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxy",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);

    assertTrue(resString.indexOf("<foo xmlns=\"http://foo\"") != -1);
  }

  @Test
  public void testProxyBodyValidation() throws Exception {
    doTestProxyValidation("http://localhost:" + dynamicPort.getNumber() + "/services/proxyBodyWithValidation");
  }

  @Test
  public void testProxyBodyValidationWithExternalSchema() throws Exception {
    doTestProxyValidation("http://localhost:" + dynamicPort.getNumber() + "/services/proxyBodyWithValidationAndSchemas");
  }

  @Test
  public void testProxyEnvelopeValidation() throws Exception {
    doTestProxyValidation("http://localhost:" + dynamicPort.getNumber() + "/services/proxyEnvelopeWithValidation");
  }

  public void doTestProxyValidation(String url) throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send(url, MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS).getRight();
    String resString = getPayloadAsString(result);
    assertTrue(resString.indexOf("Schema validation error on message") != -1);

    String valid = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body> "
        + "<echo xmlns=\"http://www.muleumo.org\">" + "  <echo>test</echo>" + "</echo>" + "</soap:Body>" + "</soap:Envelope>";
    result = client.send(url, MuleMessage.builder().payload(valid).build(), HTTP_REQUEST_OPTIONS).getRight();
    resString = getPayloadAsString(result);
    assertTrue(resString.contains("<echoResponse xmlns=\"http://www.muleumo.org\">"));
  }

  @Test
  public void testServerClientProxyWithWsdl() throws Exception {
    final Latch latch = new Latch();
    ((FunctionalTestComponent) getComponent("serverClientProxyWithWsdl"))
        .setEventCallback((context, component, muleContext) -> latch.countDown());

    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithWsdl",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    assertTrue(resString.indexOf("<test xmlns=\"http://foo\"") != -1);
  }

  @Test
  public void testServerClientProxyWithWsdl2() throws Exception {
    final Latch latch = new Latch();
    ((FunctionalTestComponent) getComponent("serverClientProxyWithWsdl2"))
        .setEventCallback((context, component, muleContext) -> latch.countDown());

    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithWsdl2",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    assertTrue(resString.indexOf("<test xmlns=\"http://foo\"") != -1);
  }

  @Test
  public void testServerClientProxyWithTransform() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithTransform",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    System.out.println(resString);
    assertTrue(resString.indexOf("<transformed xmlns=\"http://foo\">") != -1);
  }

  @Test
  public void testProxyWithDatabinding() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>"
        + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/greeter-databinding-proxy",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertTrue(resString.indexOf("greetMeResponse") != -1);
  }

  @Test
  public void testProxyWithFault() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><invalid xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></invalid>"
        + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/greeter-proxy",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);

    assertFalse("Status code should not be 'OK' when the proxied endpoint returns a fault",
                String.valueOf(OK.getStatusCode()).equals(result.getOutboundProperty("http.status")));

    assertTrue(resString.indexOf("Fault") != -1);
  }

  @Test
  public void testProxyWithIntermediateTransform() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>"
        + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/transform-proxy",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertTrue(resString.indexOf("greetMeResponse") != -1);
  }

  @Test
  public void testServerNoSoapAction() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";
    String path = "/services/routeBasedOnNoSoapAction";
    String expectedString = "<test xmlns=\"http://foo\"";

    // wsdl has soap action as empty string
    MuleMessage result = executeSoapCall(msg, "", path);
    assertResultContains(result, expectedString);

    result = executeSoapCall(msg, null, path);
    assertResultContains(result, expectedString);
  }

  @Test
  public void testServerNoSoapActionSpoofing() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";
    // wsdl has soap action as empty string so being anything else is not allowed
    MuleMessage result = executeSoapCall(msg, "echo", "/services/routeBasedOnNoSoapAction");
    assertResultIsFault(result);
  }

  @Test
  public void testServerSoapAction() throws Exception {
    String msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    MuleMessage result = executeSoapCall(msg, "EchoOperation1", "/services/routeBasedOnSoapAction");
    assertResultContains(result, "<new:parameter1");
  }

  @Test
  public void testServerSoapActionSpoofing() throws Exception {
    String msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    MuleMessage result = executeSoapCall(msg, "NonSpecifiedOperation", "/services/routeBasedOnSoapAction");
    assertResultIsFault(result);
  }

  @Test
  public void testServerNoSoapActionNoWsdl() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";
    String path = "/services/routeBasedOnNoSoapActionNoWsdl";
    String expectedString = "<test xmlns=\"http://foo\"";

    MuleMessage result = executeSoapCall(msg, "", path);
    assertResultContains(result, expectedString);

    result = executeSoapCall(msg, null, path);
    assertResultContains(result, expectedString);

    msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    result = executeSoapCall(msg, "", path);
    assertResultContains(result, "<new:parameter1");

    result = executeSoapCall(msg, null, path);
    assertResultContains(result, "<new:parameter1");
  }

  @Test
  public void testServerSoapActionNoWsdl() throws Exception {
    String path = "/services/routeBasedNoWsdl";

    msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";
    MuleMessage result = executeSoapCall(msg, "EchoOperation1", path);
    assertResultContains(result, "<new:parameter1");
  }

  private MuleMessage executeSoapCall(String msg, String soapAction, String path) throws MuleException {
    Map<String, Serializable> props = new HashMap<>();
    if (soapAction != null) {
      props.put("SOAPAction", soapAction);
    }

    MuleClient client = muleContext.getClient();
    return client.send("http://localhost:" + dynamicPort.getNumber() + path,
                       MuleMessage.builder().payload(msg).outboundProperties(props).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
  }

  private void assertResultContains(MuleMessage result, String expectedString) throws Exception {
    String resString = getPayloadAsString(result);
    System.out.println(resString);
    assertTrue("message didn't contain the test string: " + expectedString + " but was: " + resString,
               resString.indexOf(expectedString) != -1);
  }

  private void assertResultIsFault(MuleMessage result) throws Exception {
    String resString = getPayloadAsString(result);
    assertFalse("Status code should not be 'OK' when the proxied endpoint returns a fault",
                String.valueOf(OK.getStatusCode()).equals(result.getOutboundProperty("http.status")));
    assertTrue(resString.indexOf("Fault") != -1);
  }

  @Test
  public void testOneWaySendWithSoapAction() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/onewayWithSoapAction",
                                     MuleMessage.builder().payload(prepareOneWayTestMessage())
                                         .outboundProperties(prepareOneWayWithSoapActionTestProperties()).build(),
                                     HTTP_REQUEST_OPTIONS)
        .getRight();
    assertEquals("", getPayloadAsString(result));

    AsyncServiceWithSoapAction component = (AsyncServiceWithSoapAction) getComponent("asyncServiceWithSoapAction");
    assertTrue(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWayDispatchWithSoapAction() throws Exception {
    MuleClient client = muleContext.getClient();

    client.dispatch("http://localhost:" + dynamicPort.getNumber() + "/services/onewayWithSoapAction", MuleMessage.builder()
        .payload(prepareOneWayTestMessage()).outboundProperties(prepareOneWayWithSoapActionTestProperties()).build(),
                    HTTP_REQUEST_OPTIONS);

    AsyncServiceWithSoapAction component = (AsyncServiceWithSoapAction) getComponent("asyncServiceWithSoapAction");
    assertTrue(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWaySendWithSoapActionSpoofing() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/onewayWithSoapAction",
                                     MuleMessage.builder().payload(prepareOneWayTestMessage())
                                         .outboundProperties(prepareOneWaySpoofingTestProperties()).build(),
                                     HTTP_REQUEST_OPTIONS)
        .getRight();
    assertNotNull(result);

    AsyncServiceWithSoapAction component = (AsyncServiceWithSoapAction) getComponent("asyncServiceWithSoapAction");
    assertFalse(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWayDispatchWithSoapActionSpoofing() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch(
                    "http://localhost:" + dynamicPort.getNumber() + "/services/onewayWithSoapAction", MuleMessage.builder()
                        .payload(prepareOneWayTestMessage()).outboundProperties(prepareOneWaySpoofingTestProperties()).build(),
                    HTTP_REQUEST_OPTIONS);

    AsyncServiceWithSoapAction component = (AsyncServiceWithSoapAction) getComponent("asyncServiceWithSoapAction");
    assertFalse(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWaySendUnknownSoapAction() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/oneway",
                                     MuleMessage.builder().payload(prepareOneWayTestMessage())
                                         .outboundProperties(prepareOneWayWithSoapActionTestProperties()).build(),
                                     HTTP_REQUEST_OPTIONS)
        .getRight();
    assertNotNull(result);

    AsyncService component = (AsyncService) getComponent("asyncService");
    assertFalse(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWayDispatchUnknownSoapAction() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("http://localhost:" + dynamicPort.getNumber() + "/services/oneway", MuleMessage.builder()
        .payload(prepareOneWayTestMessage()).outboundProperties(prepareOneWayWithSoapActionTestProperties()).build(),
                    HTTP_REQUEST_OPTIONS);

    AsyncService component = (AsyncService) getComponent("asyncService");
    assertFalse(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWaySend() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/oneway", MuleMessage.builder()
        .payload(prepareOneWayTestMessage()).outboundProperties(prepareOneWayTestProperties()).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    assertNotNull(result);

    AsyncService component = (AsyncService) getComponent("asyncService");
    assertTrue(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWayDispatch() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch(
                    "http://localhost:" + dynamicPort.getNumber() + "/services/oneway", MuleMessage.builder()
                        .payload(prepareOneWayTestMessage()).outboundProperties(prepareOneWayTestProperties()).build(),
                    HTTP_REQUEST_OPTIONS);

    AsyncService component = (AsyncService) getComponent("asyncService");
    assertTrue(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  /**
   * MULE-4549 ReversibleXMLStreamReader chokes on comments with ClassCastException
   *
   * @throws Exception
   */
  @Test
  public void testProxyWithCommentInRequest() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/envelope-proxy",
                                     MuleMessage.builder().payload(msgWithComment).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertTrue(resString.contains(doGoogleSearch));
  }

  /**
   * MULE-6188: ReversibleXMLStreamReader throw NPE after reset because current event is null.
   *
   * @throws Exception
   */
  @Test
  public void testProxyEnvelopeWithXsltTransformation() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/envelope-xslt-proxy",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    assertTrue(getPayloadAsString(result).contains(msg));
  }

  @Test
  public void testProxyCDATA() throws Exception {
    String servicePayload =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header/><soapenv:Body><int:test/></soapenv:Body></soapenv:Envelope>";

    String msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sup=\"http://support.cxf.module.runtime.mule.org/\">\n"
            + "<soapenv:Header/>\n" + "<soapenv:Body>\n" + "<sup:invoke>\n" + "<soapenv:Envelope>\n" + "<soapenv:Header/>\n"
            + "<soapenv:Body>\n" + "<sup:invoke>\n" + "<Request>\n" + "<servicePayload><![CDATA[" + servicePayload
            + "]]></servicePayload>\n" + "</Request>\n" + "</sup:invoke>\n" + "</soapenv:Body>\n" + "</soapenv:Envelope>\n"
            + "</sup:invoke>\n" + "</soapenv:Body>\n" + "</soapenv:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/CDATAService",
                                     MuleMessage.builder().payload(msg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    assertNotNull(result);
    assertThat(unescapeXml(getPayloadAsString(result)), containsString(servicePayload));
  }

  /** MULE-6159: Proxy service fails when WSDL has faults **/
  @Test
  public void testProxyWithSoapFault() throws Exception {
    MuleClient client = muleContext.getClient();

    String proxyFaultMsg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_fault/types\"><requestType>Dan</requestType></greetMe>"
        + "</soap:Body>" + "</soap:Envelope>";

    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/proxyFault",
                                     MuleMessage.builder().payload(proxyFaultMsg).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    String resString = getPayloadAsString(result);
    assertTrue(resString.contains("ERROR"));
  }

  @Test
  public void testProxyJms() throws Exception {
    String payload = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" + "<soap:Body>"
        + "<emp:addEmployee xmlns:emp=\"http://employee.example.mule.org/\">" + "<emp:employee>"
        + "<emp:division>Runtime</emp:division>" + "<emp:name>Pepe</emp:name>" + "</emp:employee>" + "</emp:addEmployee>"
        + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/proxyJms",
                                     MuleMessage.builder().payload(payload).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is(payload));
  }

  @Test
  public void testProxyOneWay() throws Exception {
    String body = "<emp:addEmployee xmlns:emp=\"http://employee.example.mule.org/\">" + "<emp:employee>"
        + "<emp:division>Runtime</emp:division>" + "<emp:name>Pepe</emp:name>" + "</emp:employee>" + "</emp:addEmployee>";
    String payload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body>" + body
        + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/proxyOneWay",
                                     MuleMessage.builder().payload(payload).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    assertThat(result, is(notNullValue()));
    assertThat(result.getInboundProperty(HTTP_STATUS_PROPERTY), is(ACCEPTED.getStatusCode()));
    assertThat(getPayloadAsString(result), is(""));
  }

  @Test
  public void testProxyOneWayFault() throws Exception {
    String payload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body>"
        + "<emp:addEmployee xmlns:emp=\"http://employee.example.mule.org/\">" + "<emp:employee>"
        + "<emp:division>Runtime</emp:division>" + "<emp:name>Pepe</emp:name>" + "</emp:employee>" + "</emp:addEmployee>"
        + "</soap:Body>" + "</soap:Envelope>";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/proxyOneWayFault",
                                     MuleMessage.builder().payload(payload).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), containsString("ERROR"));
  }

  protected String prepareOneWayTestMessage() {
    return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body>"
        + "<ns:send xmlns:ns=\"http://testmodels.cxf.module.runtime.mule.org/\"><text>hello</text></ns:send>" + "</soap:Body>"
        + "</soap:Envelope>";
  }

  protected Map<String, Serializable> prepareOneWayTestProperties() {
    Map<String, Serializable> props = new HashMap<>();
    props.put("SOAPAction", "");
    return props;
  }

  protected Map<String, Serializable> prepareOneWayWithSoapActionTestProperties() {
    Map<String, Serializable> props = new HashMap<>();
    props.put("SOAPAction", "send");
    return props;
  }

  protected Map<String, Serializable> prepareOneWaySpoofingTestProperties() {
    Map<String, Serializable> props = new HashMap<>();
    props.put("SOAPAction", "hiddenAction");
    return props;
  }
}
