/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.util.Map;

import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Rule;
import org.junit.Test;


public class WsCustomValidatorTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/cxf/wssec/ws-custom-validator-config.xml";
    }

    @Test
    public void testSuccessfulAuthentication() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");
        MuleMessage request = new DefaultMuleMessage("me", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("vm://greetMe", request);

        assertNotNull(received);
        assertEquals("Hello me", received.getPayloadAsString());

    }

    @Test
    public void testFailAuthentication() throws Exception
    {
        ClientPasswordCallback.setPassword("wrongPassword");
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("vm://greetMe", request);

        assertNotNull(received);
        assertNotNull(received.getExceptionPayload());
        assertTrue(received.getExceptionPayload().getException().getCause() instanceof SOAPFaultException);
        assertTrue(((SOAPFaultException) received.getExceptionPayload().getException().getCause()).getMessage().contains("The security token could not be authenticated"));
    }


}
