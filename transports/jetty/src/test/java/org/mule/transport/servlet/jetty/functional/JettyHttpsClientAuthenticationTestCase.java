/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

public class JettyHttpsClientAuthenticationTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpsPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "jetty-https-client-authentication-test.xml";
    }

    @Test
    public void acceptsClientWithAuthentication() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://authenticatedClientEndpoint", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, response.getPayloadAsString());
    }

    @Test
    public void rejectsClientWithoutAuthentication() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://notAuthenticatedClientEndpoint", TEST_MESSAGE, null);
        assertEquals(NullPayload.getInstance(), response.getPayload());
        assertTrue(response.getExceptionPayload().getException().getCause() instanceof IOException);
    }

}
