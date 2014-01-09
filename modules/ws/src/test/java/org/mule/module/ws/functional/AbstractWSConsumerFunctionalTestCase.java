/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.ws.consumer.SoapFaultException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;

import org.junit.Rule;

public class AbstractWSConsumerFunctionalTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Rule
    public SystemProperty baseDir = new SystemProperty("baseDir", ClassUtils.getClassPathRoot(getClass()).getPath());

    protected static final String ECHO_REQUEST = "<tns:echo xmlns:tns=\"http://consumer.ws.module.mule.org/\"><text>Hello</text></tns:echo>";


    protected void assertValidResponse(String address) throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(address, ECHO_REQUEST, null);
        assertTrue(response.getPayloadAsString().contains("<text>Hello</text>"));
    }

    protected void assertSoapFault(String address, String expectedFaultCode) throws Exception
    {
        assertSoapFault(address, ECHO_REQUEST, expectedFaultCode);
    }

    protected void assertSoapFault(String address, String message, String expectedFaultCode) throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(address, message, null);
        assertEquals(NullPayload.getInstance(), response.getPayload());
        SoapFaultException exception =  (SoapFaultException) response.getExceptionPayload().getException().getCause();
        assertEquals(expectedFaultCode, exception.getFaultCode().getLocalPart());
    }
}
