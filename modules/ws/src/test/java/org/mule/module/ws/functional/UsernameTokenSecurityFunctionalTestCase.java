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
import org.junit.Test;


public class UsernameTokenSecurityFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Rule
    public SystemProperty baseDir = new SystemProperty("baseDir", ClassUtils.getClassPathRoot(getClass()).getPath());

    private static final String ECHO_REQUEST = "<tns:echo xmlns:tns=\"http://consumer.ws.module.mule.org/\"><text>Hello</text></tns:echo>";

    @Override
    protected String getConfigFile()
    {
        return "username-token-security-config.xml";
    }

    @Test
    public void requestWithValidCredentialsReturnsExpectedResult() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://clientWithValidCredentials", ECHO_REQUEST, null);
        assertTrue(response.getPayloadAsString().contains("<text>Hello</text>"));
    }

    @Test
    public void requestWithInvalidCredentialsReturnsFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://clientWithInvalidCredentials", ECHO_REQUEST, null);
        assertEquals(NullPayload.getInstance(), response.getPayload());
        assertTrue(response.getExceptionPayload().getException().getCause() instanceof SoapFaultException);
    }

    @Test
    public void requestWithoutCredentialsReturnsFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://clientWithoutCredentials", ECHO_REQUEST, null);
        assertEquals(NullPayload.getInstance(), response.getPayload());
        assertTrue(response.getExceptionPayload().getException().getCause() instanceof SoapFaultException);
    }


}
