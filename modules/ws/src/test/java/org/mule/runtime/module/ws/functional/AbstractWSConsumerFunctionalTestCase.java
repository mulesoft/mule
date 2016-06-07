/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;


import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mule.runtime.module.ws.functional.SoapFaultCodeMatcher.hasFaultCode;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.module.ws.consumer.SoapFaultException;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class AbstractWSConsumerFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected static final String ECHO_REQUEST = "<tns:echo xmlns:tns=\"http://consumer.ws.module.runtime.mule.org/\">" +
                                                 "<text>Hello</text></tns:echo>";

    protected static final String EXPECTED_ECHO_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                                           "<ns2:echoResponse xmlns:ns2=\"http://consumer.ws.module.runtime.mule.org/\">" +
                                                           "<text>Hello</text></ns2:echoResponse>";

    protected void assertValidResponse(String flowName) throws Exception
    {
        assertValidResponse(flowName, Collections.emptyMap());
    }

    protected void assertValidResponse(String flowName, Map<String, Serializable> properties) throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(ECHO_REQUEST, properties, muleContext);
        assertValidResponse(flowName, request);
    }

    protected void assertValidResponse(String flowName, MuleMessage message) throws Exception
    {
        MuleMessage response = flowRunner(flowName).withPayload(message).run().getMessage();
        assertXMLEqual(EXPECTED_ECHO_RESPONSE, getPayloadAsString(response));
    }

    protected void assertSoapFault(String flowName, String expectedFaultCode) throws Exception
    {
        assertSoapFault(flowName, ECHO_REQUEST, expectedFaultCode);
    }

    protected void assertSoapFault(String flowName, String message, String expectedFaultCode) throws Exception
    {
        assertSoapFault(flowName, message, null, expectedFaultCode);
    }

    protected void assertSoapFault(String flowName, String message, Map<String, Serializable> properties, String expectedFaultCode) throws Exception
    {
        expectedException.expect(SoapFaultException.class);
        expectedException.expect(hasFaultCode(expectedFaultCode));
        flowRunner(flowName).withPayload(new DefaultMuleMessage(message, properties, muleContext)).run().getMessage();
    }

}
