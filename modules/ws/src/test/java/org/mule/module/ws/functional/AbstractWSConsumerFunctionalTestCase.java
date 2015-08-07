/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;


import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.ws.consumer.SoapFaultException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractWSConsumerFunctionalTestCase extends FunctionalTestCase
{

    public static final String USE_TRANSPORT_FOR_URIS = "useTransportForUris";
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    protected static final String ECHO_REQUEST = "<tns:echo xmlns:tns=\"http://consumer.ws.module.mule.org/\">" +
                                                 "<text>Hello</text></tns:echo>";

    protected static final String EXPECTED_ECHO_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                                           "<ns2:echoResponse xmlns:ns2=\"http://consumer.ws.module.mule.org/\">" +
                                                           "<text>Hello</text></ns2:echoResponse>";


    @Parameterized.Parameter(value = 0)
    public boolean useTransportForUris;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[] {true}, new Object[] {false});
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        System.setProperty(USE_TRANSPORT_FOR_URIS, Boolean.toString(useTransportForUris));
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        System.clearProperty(USE_TRANSPORT_FOR_URIS);
    }


    protected void assertValidResponse(String address) throws Exception
    {
        assertValidResponse(address, null);
    }

    protected void assertValidResponse(String address, Map<String, Object> properties) throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(address, ECHO_REQUEST, properties);
        assertXMLEqual(EXPECTED_ECHO_RESPONSE, response.getPayloadAsString());
    }

    protected void assertSoapFault(String address, String expectedFaultCode) throws Exception
    {
        assertSoapFault(address, ECHO_REQUEST, expectedFaultCode);
    }

    protected void assertSoapFault(String address, String message, String expectedFaultCode) throws Exception
    {
        assertSoapFault(address, message, null, expectedFaultCode);
    }

    protected void assertSoapFault(String address, String message, Map<String, Object> properties, String expectedFaultCode) throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(address, message, properties);
        assertEquals(NullPayload.getInstance(), response.getPayload());
        SoapFaultException exception = (SoapFaultException) response.getExceptionPayload().getException();
        assertEquals(expectedFaultCode, exception.getFaultCode().getLocalPart());
    }
}
