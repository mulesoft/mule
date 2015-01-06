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
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WsSecurityConfigMelExpressionTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameterized.Parameter(0)
    public String config;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"org/mule/module/cxf/wssec/ws-security-config-mel-expression-config.xml"},
                {"org/mule/module/cxf/wssec/ws-security-config-mel-expression-config-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return config;
    }

    @Test
    public void testSuccessfulAuthentication() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");
        MuleMessage request = new DefaultMuleMessage("PasswordText", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://greetMe", request);

        assertNotNull(received);
        assertEquals("Hello PasswordText", received.getPayloadAsString());
    }

    @Test
    public void testFailAuthentication() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");
        MuleMessage request = new DefaultMuleMessage("UnknownPasswordEncoding", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://greetMe", request);

        assertNotNull(received);
        assertNotNull(received.getExceptionPayload());
        assertTrue(received.getExceptionPayload().getException().getCause() instanceof SOAPFaultException);
        assertTrue(((SOAPFaultException) received.getExceptionPayload().getException().getCause()).getMessage().contains("Security processing failed"));
    }
}
