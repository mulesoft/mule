/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.ws.consumer.SoapFaultException;

import org.junit.Test;

public class CatchExceptionStrategyFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "catch-exception-strategy-config.xml";
    }

    @Test
    public void soapFaultThrowsException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://soapFaultWithoutCatchExceptionStrategy", "<tns:fail xmlns:tns=\"http://consumer.ws.module.mule.org/\"><text>Hello</text></tns:fail>", null);

        assertNotNull(response.getExceptionPayload());
        SoapFaultException soapFault = (SoapFaultException) response.getExceptionPayload().getException().getCause();
        assertEquals("Hello", soapFault.getMessage());
        assertEquals("Server", soapFault.getFaultCode().getLocalPart());
    }

    @Test
    public void catchExceptionStrategyHandlesSoapFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://soapFaultWithCatchExceptionStrategy", "<tns:fail xmlns:tns=\"http://consumer.ws.module.mule.org/\"><text>Hello</text></tns:fail>", null);
        assertEquals("EXCEPTION", response.getPayloadAsString());
    }

}
