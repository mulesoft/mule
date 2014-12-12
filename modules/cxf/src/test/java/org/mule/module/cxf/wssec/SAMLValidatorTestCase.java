/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.runners.Parameterized.*;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SAMLValidatorTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameter
    public String configFile;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"org/mule/module/cxf/wssec/saml-validator-conf.xml"},
                {"org/mule/module/cxf/wssec/saml-validator-conf-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void testSAMLUnsignedAssertion() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("me", (Map<String, Object>) null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://greetMe", request);

        assertNotNull(received);
        assertEquals("Hello me", received.getPayloadAsString());
    }

    @Test
    public void testSAMLSignedAssertion() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("me", (Map<String, Object>) null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://greetMeSigned", request);

        assertNotNull(received);
        assertEquals("Hello me", received.getPayloadAsString());
    }

    public static class PasswordCallbackHandler implements CallbackHandler
    {
        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
        {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

            pc.setPassword("secret");
        }
    }
}
