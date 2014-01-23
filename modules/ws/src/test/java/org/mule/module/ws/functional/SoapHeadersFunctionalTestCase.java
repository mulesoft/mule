/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.tck.functional.EventCallback;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class SoapHeadersFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    private static final String ECHO_HEADERS_REQUEST = "<tns:echoWithHeaders xmlns:tns=\"http://consumer.ws.module.mule.org/\">" +
                                                       "<text>Hello</text></tns:echoWithHeaders>";

    private static final String REQUEST_HEADER_IN = "<headerIn xmlns=\"http://consumer.ws.module.mule.org/\">TEST_HEADER_1</headerIn>";

    private static final String REQUEST_HEADER_INOUT = "<headerInOut xmlns=\"http://consumer.ws.module.mule.org/\">TEST_HEADER_2</headerInOut>";

    private static final String RESPONSE_HEADER_OUT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                                      "<ns2:headerOut xmlns:ns2=\"http://consumer.ws.module.mule.org/\" " +
                                                      "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                                      "TEST_HEADER_1 OUT</ns2:headerOut>";

    private static final String RESPONSE_HEADER_INOUT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                                        "<ns2:headerInOut xmlns:ns2=\"http://consumer.ws.module.mule.org/\" " +
                                                        "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                                        "TEST_HEADER_2 INOUT</ns2:headerInOut>";

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    @Override
    protected String getConfigFile()
    {
        return "soap-headers-config.xml";
    }

    @Test
    public void messagePropertiesAreMappedToSoapHeaders() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlow");

        MuleEvent event = getTestEvent(ECHO_HEADERS_REQUEST);
        event.getMessage().setProperty("soap.headerIn", REQUEST_HEADER_IN, PropertyScope.OUTBOUND);
        event.getMessage().setProperty("soap.headerInOut", REQUEST_HEADER_INOUT, PropertyScope.OUTBOUND);

        event = flow.process(event);

        assertEquals(RESPONSE_HEADER_OUT, event.getMessage().getInboundProperty("soap.headerOut"));
        assertEquals(RESPONSE_HEADER_INOUT, event.getMessage().getInboundProperty("soap.headerInOut"));

    }

    @Test
    public void soapHeadersAreRemovedFromMessage() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlow");

        MuleEvent event = getTestEvent(ECHO_HEADERS_REQUEST);

        event.getMessage().setProperty("soap.headerIn", REQUEST_HEADER_IN, PropertyScope.OUTBOUND);
        event.getMessage().setProperty("soap.headerInOut", REQUEST_HEADER_INOUT, PropertyScope.OUTBOUND);
        event.getMessage().setProperty("testHttpHeader", TEST_MESSAGE, PropertyScope.OUTBOUND);

        // A test component is used on the server side to check HTTP headers that are received (inbound properties).

        getFunctionalTestComponent("server").setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                errorCollector.checkThat(context.getMessage().getInboundProperty("testHttpHeader"), notNullValue());
                errorCollector.checkThat(context.getMessage().getInboundProperty("soap.headerIn"), nullValue());
                errorCollector.checkThat(context.getMessage().getInboundProperty("soap.headerInOut"), nullValue());
            }
        });

        flow.process(event);
    }


    @Test(expected = DispatchException.class)
    public void failToRouteEventWithInvalidSoapHeader() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlow");

        MuleEvent event = getTestEvent(ECHO_HEADERS_REQUEST);
        event.getMessage().setProperty("soap.header", "invalid xml", PropertyScope.OUTBOUND);

        flow.process(event);
    }

}
