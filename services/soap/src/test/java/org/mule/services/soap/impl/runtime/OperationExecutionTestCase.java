/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.runtime;

import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.services.soap.api.message.SoapRequest.builder;
import org.mule.services.soap.api.client.SoapClient;
import org.mule.services.soap.api.message.SoapRequest;
import org.mule.services.soap.api.message.SoapResponse;
import org.mule.services.soap.impl.AbstractSoapServiceTestCase;
import org.mule.services.soap.impl.TestSoapClient;
import org.mule.services.soap.api.message.ImmutableSoapRequest;

import com.google.common.collect.ImmutableMap;

import java.net.URL;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("Operation Execution")
public class OperationExecutionTestCase extends AbstractSoapServiceTestCase {

  @Test
  @Description("Consumes an operation that expects a simple type and returns a simple type")
  public void simpleOperation() throws Exception {
    testSimpleOperation(client);
  }

  @Test
  @Description("Consumes an operation using a connection that uses a local .wsdl file")
  public void echoWithLocalWsdl() throws Exception {
    URL wsdl = currentThread().getContextClassLoader().getResource("wsdl/simple-service.wsdl");
    TestSoapClient localWsdlClient = new TestSoapClient(wsdl.getPath(), defaultAddress, soapVersion);
    testSimpleOperation(localWsdlClient);
  }

  private void testSimpleOperation(SoapClient client) throws Exception {
    ImmutableSoapRequest req = builder().withContent("<con:echo xmlns:con=\"http://service.impl.soap.services.mule.org/\">\n"
        + "    <text>test</text>\n"
        + "</con:echo>").ofContentType(APPLICATION_XML).withOperation("echo")
        .build();
    SoapResponse response = client.consume(req);
    assertThat(response.getSoapHeaders().isEmpty(), is(true));
    assertSimilarXml("<ns2:echoResponse xmlns:ns2=\"http://service.impl.soap.services.mule.org/\">\n"
        + "    <text>test response</text>\n"
        + "</ns2:echoResponse>", response.getContent());
  }

  @Test
  @Description("Consumes an operation that expects an input and a set of headers and returns a simple type and a set of headers")
  public void simpleOperationWithHeaders() throws Exception {

    ImmutableMap<String, String> soapHeaders =
        ImmutableMap.<String, String>builder().put("headerIn", HEADER_IN).put("headerInOut", HEADER_INOUT).build();

    ImmutableSoapRequest req =
        builder().withContent("<con:echoWithHeaders xmlns:con=\"http://service.impl.soap.services.mule.org/\">\n"
            + "    <text>test</text>\n"
            + "</con:echoWithHeaders>")
            .withOperation("echoWithHeaders")
            .withSoapHeaders(soapHeaders)
            .ofContentType(APPLICATION_XML)
            .build();

    SoapResponse response = client.consume(req);

    assertSimilarXml(response.getSoapHeaders().get("headerOut"), HEADER_OUT);
    assertSimilarXml(response.getSoapHeaders().get("headerInOut"), HEADER_INOUT_RES);

    assertSimilarXml("<ns2:echoWithHeadersResponse xmlns:ns2=\"http://service.impl.soap.services.mule.org/\">\n" +
        "<text>test response</text>" +
        "</ns2:echoWithHeadersResponse>", response.getContent());
  }

  @Test
  @Description("Consumes an operation that expects 2 parameters (a simple one and a complex one) and returns a complex type")
  public void complexTypeOperation() throws Exception {
    ImmutableSoapRequest req =
        builder().withContent("<con:echoAccount xmlns:con=\"http://service.impl.soap.services.mule.org/\">\n"
            + "    <account>\n"
            + "        <id>12</id>\n"
            + "        <items>chocolate</items>\n"
            + "        <items>banana</items>\n"
            + "        <items>dulce de leche</items>\n"
            + "        <startingDate>2016-09-23T00:00:00-03:00</startingDate>\n"
            + "    </account>\n"
            + "    <name>Juan</name>\n"
            + "</con:echoAccount>").withOperation("echoAccount").build();
    SoapResponse response = client.consume(req);
    assertThat(response.getSoapHeaders().isEmpty(), is(true));
    assertSimilarXml("<ns2:echoAccountResponse xmlns:ns2=\"http://service.impl.soap.services.mule.org/\">\n"
        + "    <account>\n"
        + "        <clientName>Juan</clientName>\n"
        + "        <id>12</id>\n"
        + "        <items>chocolate</items>\n"
        + "        <items>banana</items>\n"
        + "        <items>dulce de leche</items>\n"
        + "        <startingDate>2016-09-23T00:00:00-03:00</startingDate>\n"
        + "    </account>\n"
        + "</ns2:echoAccountResponse>", response.getContent());
  }

  @Test
  @Description("Consumes an operation that expects no parameters and returns a simple type")
  public void noParamsOperation() throws Exception {
    SoapRequest req = builder()
        .withContent("<con:noParams xmlns:con=\"http://service.impl.soap.services.mule.org/\"/>")
        .withOperation("noParams")
        .build();
    testNoParams(req);
  }

  @Test
  @Description("Consumes an operation that expects no parameters auto-generating the request and returns a simple type")
  public void noParamsOperationWithoutXmlPayload() throws Exception {
    testNoParams(SoapRequest.empty("noParams"));
  }

  private void testNoParams(SoapRequest request) throws Exception {
    SoapResponse response = client.consume(request);
    assertThat(response.getSoapHeaders().isEmpty(), is(true));
    assertSimilarXml("<ns2:noParamsResponse xmlns:ns2=\"http://service.impl.soap.services.mule.org/\">"
        + "    <text>response</text>"
        + "</ns2:noParamsResponse>", response.getContent());
  }
}
