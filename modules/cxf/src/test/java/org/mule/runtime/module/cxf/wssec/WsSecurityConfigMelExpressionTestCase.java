/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.wssec;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WsSecurityConfigMelExpressionTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "org/mule/runtime/module/cxf/wssec/ws-security-config-mel-expression-config-httpn.xml";
    }

    @Test
    public void testSuccessfulAuthentication() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");
        MuleMessage received = flowRunner("cxfClient").withPayload(getTestMuleMessage("PasswordText")).run().getMessage();

        assertNotNull(received);
        assertEquals("Hello PasswordText", getPayloadAsString(received));
    }

    @Test
    public void testFailAuthentication() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");
        expectedException.expectCause(instanceOf(SOAPFaultException.class));
        expectedException.expectMessage("Security exception occurred invoking web service");
        flowRunner("cxfClient").withPayload(getTestMuleMessage("UnknownPasswordEncoding")).run().getMessage();
    }
}
