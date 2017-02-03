/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeXml;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.compatibility.module.cxf.SoapConstants.SOAP_ACTION_PROPERTY_CAPS;
import static org.mule.service.http.api.HttpConstants.HttpStatus.ACCEPTED;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.compatibility.module.cxf.testmodels.AsyncService;
import org.mule.compatibility.module.cxf.testmodels.AsyncServiceWithSoapAction;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.exception.MuleException;
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

import org.junit.Rule;
import org.junit.Test;

public class ProxyTestCase extends AbstractCxfOverHttpExtensionTestCase {

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

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getConfigFile() {
    return "proxy-conf-flow-httpn.xml";
  }

  @Test
  public void testServerWithEcho() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/Echo")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.indexOf("<test xmlns=\"http://foo\"> foo </test>") != -1);
  }

  @Test
  public void testServerClientProxy() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <foo xmlns=\"http://foo\"></foo>" + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/proxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.indexOf("<foo xmlns=\"http://foo\"") != -1);

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
    HttpRequest request = HttpRequest.builder().setUri(url).setMethod(POST)
        .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.indexOf("Schema validation error on message") != -1);

    String valid = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body> "
        + "<echo xmlns=\"http://www.muleumo.org\">" + "  <echo>test</echo>" + "</echo>" + "</soap:Body>" + "</soap:Envelope>";
    request = HttpRequest.builder().setUri(url).setMethod(POST)
        .setEntity(new ByteArrayHttpEntity(valid.getBytes())).build();

    response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains("<echoResponse xmlns=\"http://www.muleumo.org\">"));
  }

  @Test
  public void testServerClientProxyWithWsdl() throws Exception {
    final Latch latch = new Latch();
    ((FunctionalTestComponent) getComponent("serverClientProxyWithWsdl"))
        .setEventCallback((context, component, muleContext) -> latch.countDown());

    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request = HttpRequest.builder()
        .setUri("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithWsdl").setMethod(POST)
        .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.indexOf("<test xmlns=\"http://foo\"") != -1);
  }

  @Test
  public void testServerClientProxyWithWsdl2() throws Exception {
    final Latch latch = new Latch();
    ((FunctionalTestComponent) getComponent("serverClientProxyWithWsdl2"))
        .setEventCallback((context, component, muleContext) -> latch.countDown());

    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithWsdl2")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    assertTrue(payload.indexOf("<test xmlns=\"http://foo\"") != -1);
  }

  @Test
  public void testServerClientProxyWithTransform() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithTransform")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.indexOf("<transformed xmlns=\"http://foo\">") != -1);
  }

  @Test
  public void testProxyWithDatabinding() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>"
        + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/greeter-databinding-proxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.indexOf("greetMeResponse") != -1);

  }

  @Test
  public void testProxyWithFault() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><invalid xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></invalid>"
        + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/greeter-proxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertFalse("Status code should not be 'OK' when the proxied endpoint returns a fault",
                OK.getStatusCode() == response.getStatusCode());

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.indexOf("Fault") != -1);
  }

  @Test
  public void testProxyWithIntermediateTransform() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>"
        + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/transform-proxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.indexOf("greetMeResponse") != -1);
  }

  @Test
  public void testServerNoSoapAction() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";
    String path = "/services/routeBasedOnNoSoapAction";
    String expectedString = "<test xmlns=\"http://foo\"";

    // wsdl has soap action as empty string
    HttpResponse response = executeSoapCall(msg, "", path);
    assertResultContains(response, expectedString);

    response = executeSoapCall(msg, null, path);
    assertResultContains(response, expectedString);
  }

  @Test
  public void testServerNoSoapActionSpoofing() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";
    // wsdl has soap action as empty string so being anything else is not allowed
    HttpResponse response = executeSoapCall(msg, "echo", "/services/routeBasedOnNoSoapAction");
    assertResultIsFault(response);
  }

  @Test
  public void testServerSoapAction() throws Exception {
    String msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    HttpResponse response = executeSoapCall(msg, "EchoOperation1", "/services/routeBasedOnSoapAction");
    assertResultContains(response, "<new:parameter1");
  }

  @Test
  public void testServerSoapActionSpoofing() throws Exception {
    String msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    HttpResponse response = executeSoapCall(msg, "NonSpecifiedOperation", "/services/routeBasedOnSoapAction");
    assertResultIsFault(response);
  }

  @Test
  public void testServerNoSoapActionNoWsdl() throws Exception {
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";
    String path = "/services/routeBasedOnNoSoapActionNoWsdl";
    String expectedString = "<test xmlns=\"http://foo\"";

    HttpResponse response = executeSoapCall(msg, "", path);
    assertResultContains(response, expectedString);

    response = executeSoapCall(msg, null, path);
    assertResultContains(response, expectedString);

    msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    response = executeSoapCall(msg, "", path);
    assertResultContains(response, "<new:parameter1");

    response = executeSoapCall(msg, null, path);
    assertResultContains(response, "<new:parameter1");
  }

  @Test
  public void testServerSoapActionNoWsdl() throws Exception {
    String path = "/services/routeBasedNoWsdl";

    msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
            + "<soapenv:Header/>" + "  <soapenv:Body>" + "    <new:parameter1>hello world</new:parameter1>" + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

    HttpResponse response = executeSoapCall(msg, "EchoOperation1", path);
    assertResultContains(response, "<new:parameter1");
  }

  private HttpResponse executeSoapCall(String msg, String soapAction, String path)
      throws MuleException, IOException, TimeoutException {
    ParameterMap headersMap = new ParameterMap();
    if (soapAction != null) {
      headersMap.put("SOAPAction", soapAction);
    }

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + path)
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes()))
            .setHeaders(headersMap).build();

    return httpClient.send(request, RECEIVE_TIMEOUT, false, null);

  }

  private void assertResultContains(HttpResponse result, String expectedString) throws Exception {
    String payload = IOUtils.toString(((InputStreamHttpEntity) result.getEntity()).getInputStream());
    System.out.println(payload);
    assertTrue("message didn't contain the test string: " + expectedString + " but was: " + payload,
               payload.indexOf(expectedString) != -1);
  }

  private void assertResultIsFault(HttpResponse result) throws Exception {
    String payload = IOUtils.toString(((InputStreamHttpEntity) result.getEntity()).getInputStream());
    assertThat(payload, containsString("Fault"));
    assertFalse("Status code should not be 'OK' when the proxied endpoint returns a fault",
                OK.getStatusCode() == result.getStatusCode());
  }

  @Test
  public void testOneWaySendWithSoapAction() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/onewayWithSoapAction")
            .setMethod(POST)
            .setHeaders(prepareOneWayWithSoapActionTestProperties())
            .setEntity(new ByteArrayHttpEntity(prepareOneWayTestMessage().getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertEquals("", payload);

    AsyncServiceWithSoapAction component = (AsyncServiceWithSoapAction) getComponent("asyncServiceWithSoapAction");
    assertTrue(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWaySendWithSoapActionSpoofing() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/onewayWithSoapAction")
            .setMethod(POST)
            .setHeaders(prepareOneWaySpoofingTestProperties())
            .setEntity(new ByteArrayHttpEntity(prepareOneWayTestMessage().getBytes())).build();

    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    AsyncServiceWithSoapAction component = (AsyncServiceWithSoapAction) getComponent("asyncServiceWithSoapAction");
    assertFalse(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWaySendUnknownSoapAction() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/oneway")
            .setMethod(POST)
            .setHeaders(prepareOneWayWithSoapActionTestProperties())
            .setEntity(new ByteArrayHttpEntity(prepareOneWayTestMessage().getBytes())).build();

    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    AsyncService component = (AsyncService) getComponent("asyncService");
    assertFalse(component.getLatch().await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testOneWaySend() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/oneway")
            .setMethod(POST)
            .setHeaders(prepareOneWayTestProperties())
            .setEntity(new ByteArrayHttpEntity(prepareOneWayTestMessage().getBytes())).build();

    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

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
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/envelope-proxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msgWithComment.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains(doGoogleSearch));
  }

  /**
   * MULE-6188: ReversibleXMLStreamReader throw NPE after reset because current event is null.
   *
   * @throws Exception
   */
  @Test
  public void testProxyEnvelopeWithXsltTransformation() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/envelope-xslt-proxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains(msg));
  }

  @Test
  public void testProxyCDATA() throws Exception {
    String servicePayload =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header/><soapenv:Body><int:test/></soapenv:Body></soapenv:Envelope>";

    String msg =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sup=\"http://support.cxf.module.compatibility.mule.org/\">\n"
            + "<soapenv:Header/>\n" + "<soapenv:Body>\n" + "<sup:invoke>\n" + "<soapenv:Envelope>\n" + "<soapenv:Header/>\n"
            + "<soapenv:Body>\n" + "<sup:invoke>\n" + "<Request>\n" + "<servicePayload><![CDATA[" + servicePayload
            + "]]></servicePayload>\n" + "</Request>\n" + "</sup:invoke>\n" + "</soapenv:Body>\n" + "</soapenv:Envelope>\n"
            + "</sup:invoke>\n" + "</soapenv:Body>\n" + "</soapenv:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/CDATAService")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(msg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertThat(unescapeXml(payload), containsString(servicePayload));
  }

  /** MULE-6159: Proxy service fails when WSDL has faults **/
  @Test
  public void testProxyWithSoapFault() throws Exception {
    String proxyFaultMsg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_fault/types\"><requestType>Dan</requestType></greetMe>"
        + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/proxyFault")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(proxyFaultMsg.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains("ERROR"));

  }

  @Test
  public void testProxyJms() throws Exception {
    String requestPayload = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" + "<soap:Body>"
        + "<emp:addEmployee xmlns:emp=\"http://employee.example.mule.org/\">" + "<emp:employee>"
        + "<emp:division>Runtime</emp:division>" + "<emp:name>Pepe</emp:name>" + "</emp:employee>" + "</emp:addEmployee>"
        + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/proxyJms")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertThat(payload, is(requestPayload));
  }

  @Test
  public void testProxyOneWay() throws Exception {
    String body = "<emp:addEmployee xmlns:emp=\"http://employee.example.mule.org/\">" + "<emp:employee>"
        + "<emp:division>Runtime</emp:division>" + "<emp:name>Pepe</emp:name>" + "</emp:employee>" + "</emp:addEmployee>";
    String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body>" + body
        + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/proxyOneWay")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertThat(response.getStatusCode(), is(ACCEPTED.getStatusCode()));
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertThat(payload, is(""));
  }

  @Test
  public void testProxyOneWayFault() throws Exception {
    String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body>"
        + "<emp:addEmployee xmlns:emp=\"http://employee.example.mule.org/\">" + "<emp:employee>"
        + "<emp:division>Runtime</emp:division>" + "<emp:name>Pepe</emp:name>" + "</emp:employee>" + "</emp:addEmployee>"
        + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/proxyOneWayFault")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity(requestPayload.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertThat(payload, containsString("ERROR"));
  }

  protected String prepareOneWayTestMessage() {
    return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body>"
        + "<ns:send xmlns:ns=\"http://testmodels.cxf.module.compatibility.mule.org/\"><text>hello</text></ns:send>"
        + "</soap:Body>"
        + "</soap:Envelope>";
  }

  protected ParameterMap prepareOneWayTestProperties() {
    ParameterMap parameterMap = new ParameterMap();
    parameterMap.put(SOAP_ACTION_PROPERTY_CAPS, "");

    return parameterMap;
  }

  protected ParameterMap prepareOneWayWithSoapActionTestProperties() {
    ParameterMap parameterMap = new ParameterMap();
    parameterMap.put(SOAP_ACTION_PROPERTY_CAPS, "send");

    return parameterMap;
  }

  protected ParameterMap prepareOneWaySpoofingTestProperties() {
    ParameterMap parameterMap = new ParameterMap();
    parameterMap.put(SOAP_ACTION_PROPERTY_CAPS, "hiddenAction");

    return parameterMap;
  }
}
