/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.wssec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;
import org.junit.Rule;
import org.junit.Test;

public class SAMLValidatorTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/runtime/module/cxf/wssec/saml-validator-conf-httpn.xml";
    }

    @Test
    public void testSAMLUnsignedAssertion() throws Exception
    {
        MuleMessage received = flowRunner("cxfClient").withPayload(getTestMuleMessage("me")).run().getMessage();

        assertNotNull(received);
        assertEquals("Hello me", getPayloadAsString(received));
    }

    @Test
    public void testSAMLSignedAssertion() throws Exception
    {
        MuleMessage received = flowRunner("cxfClientSigned").withPayload(getTestMuleMessage("me")).run().getMessage();

        assertNotNull(received);
        assertEquals("Hello me", getPayloadAsString(received));
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
