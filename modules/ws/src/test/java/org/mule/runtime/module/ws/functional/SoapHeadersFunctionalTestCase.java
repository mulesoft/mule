/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class SoapHeadersFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  private static final String SOAP_HEADER_IN = "soap.headerIn";
  private static final String SOAP_HEADER_OUT = "soap.headerOut";
  private static final String SOAP_HEADER_INOUT = "soap.headerInOut";
  private static final String HTTP_HEADER = "testHttpHeader";

  private static final String ECHO_HEADERS_REQUEST =
      "<tns:echoWithHeaders xmlns:tns=\"http://consumer.ws.module.runtime.mule.org/\">"
          + "<text>Hello</text></tns:echoWithHeaders>";

  private static final String REQUEST_HEADER_IN =
      "<headerIn xmlns=\"http://consumer.ws.module.runtime.mule.org/\">TEST_HEADER_1</headerIn>";

  private static final String REQUEST_HEADER_INOUT =
      "<headerInOut xmlns=\"http://consumer.ws.module.runtime.mule.org/\">TEST_HEADER_2</headerInOut>";

  private static final String RESPONSE_HEADER_OUT =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<ns2:headerOut xmlns:ns2=\"http://consumer.ws.module.runtime.mule.org/\" "
          + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "TEST_HEADER_1 OUT</ns2:headerOut>";

  private static final String RESPONSE_HEADER_INOUT =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<ns2:headerInOut xmlns:ns2=\"http://consumer.ws.module.runtime.mule.org/\" "
          + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "TEST_HEADER_2 INOUT</ns2:headerInOut>";

  @Rule
  public ErrorCollector errorCollector = new ErrorCollector();

  @Override
  protected String getConfigFile() {
    return "soap-headers-config.xml";
  }

  @Test
  public void messagePropertiesAreMappedToSoapHeaders() throws Exception {
    MuleEvent event =
        flowRunner("testFlow").withPayload(ECHO_HEADERS_REQUEST).withOutboundProperty(SOAP_HEADER_IN, REQUEST_HEADER_IN)
            .withOutboundProperty(SOAP_HEADER_INOUT, REQUEST_HEADER_INOUT).run();

    assertThat(event.getMessage().getInboundProperty(SOAP_HEADER_OUT), is(RESPONSE_HEADER_OUT));
    assertThat(event.getMessage().getInboundProperty(SOAP_HEADER_INOUT), is(RESPONSE_HEADER_INOUT));

  }

  @Test
  public void soapHeadersAreRemovedFromMessage() throws Exception {
    // A test component is used on the server side to check HTTP headers that are received (inbound properties).

    getFunctionalTestComponent("server").setEventCallback((context, component, muleContext) -> {
      errorCollector.checkThat(context.getMessage().getInboundProperty(HTTP_HEADER), notNullValue());
      errorCollector.checkThat(context.getMessage().getInboundProperty(SOAP_HEADER_IN), nullValue());
      errorCollector.checkThat(context.getMessage().getInboundProperty(SOAP_HEADER_INOUT), nullValue());
    });

    flowRunner("testFlow").withPayload(ECHO_HEADERS_REQUEST).withOutboundProperty(SOAP_HEADER_IN, REQUEST_HEADER_IN)
        .withOutboundProperty(SOAP_HEADER_INOUT, REQUEST_HEADER_INOUT).withOutboundProperty(HTTP_HEADER, TEST_MESSAGE).run();
  }


  @Test
  public void invalidXmlInSoapHeaderOutboundProperty() throws Exception {
    MessagingException e = flowRunner("testFlow").withPayload(ECHO_HEADERS_REQUEST)
        .withOutboundProperty(SOAP_HEADER_IN, "invalid xml").runExpectingException();

    assertThat("Expected as header can't be converted to XML", e, instanceOf(TransformerMessagingException.class));
    assertThat(e.getMessage(), containsString(SOAP_HEADER_IN));
  }

  @Test
  public void invalidSoapHeaderOutboundPropertyType() throws Exception {
    MessagingException e = flowRunner("testFlow").withPayload(ECHO_HEADERS_REQUEST)
        .withOutboundProperty(SOAP_HEADER_IN, new String()).runExpectingException();

    assertThat("Expected as header can't be converted to XML", e, instanceOf(TransformerMessagingException.class));
  }

}
