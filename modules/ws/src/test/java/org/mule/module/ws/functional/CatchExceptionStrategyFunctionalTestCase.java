/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;


import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.module.ws.consumer.SoapFaultException;
import org.mule.tck.listener.ExceptionListener;

import org.junit.Test;

public class CatchExceptionStrategyFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    private static final String FAIL_REQUEST = "<tns:fail xmlns:tns=\"http://consumer.ws.module.mule.org/\">" +
                                               "<text>Hello</text></tns:fail>";

    private static final String EXPECTED_SOAP_FAULT_DETAIL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><detail>" +
                                                             "<ns2:EchoException xmlns:ns2=\"http://consumer.ws.module.mule.org/\">" +
                                                             "<text>Hello</text></ns2:EchoException></detail>";

    @Override
    protected String getConfigFile()
    {
        return "catch-exception-strategy-config.xml";
    }

    @Test
    public void soapFaultThrowsException() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("soapFaultWithoutCatchExceptionStrategy");

        try
        {
            flow.process(getTestEvent(FAIL_REQUEST));
            fail();
        }
        catch (MessagingException e)
        {
            MuleMessage response = e.getEvent().getMessage();

            assertNotNull(response.getExceptionPayload());

            SoapFaultException soapFault = (SoapFaultException) response.getExceptionPayload().getException();
            assertTrue(soapFault.getMessage().startsWith("Hello"));
            assertEquals("Server", soapFault.getFaultCode().getLocalPart());
        }

    }

    @Test
    public void catchExceptionStrategyHandlesSoapFault() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("soapFaultWithCatchExceptionStrategy");

        ExceptionListener listener = new ExceptionListener(muleContext);
        MuleMessage response = flow.process(getTestEvent(FAIL_REQUEST)).getMessage();

        // Assert that the exception was thrown
        listener.waitUntilAllNotificationsAreReceived();

        assertXMLEqual(EXPECTED_SOAP_FAULT_DETAIL, response.getPayloadAsString());

        assertNull(response.getExceptionPayload());

        SoapFaultException soapFault = response.getOutboundProperty("soapFaultException");
        assertTrue(soapFault.getMessage().startsWith("Hello"));
        assertEquals("Server", soapFault.getFaultCode().getLocalPart());
    }

}
