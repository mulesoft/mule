/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.functional;

import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;
import org.junit.Rule;
import org.junit.Test;


public class UsernameTokenSecurityFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Rule
    public SystemProperty textPassword = new SystemProperty("textPasswordPlaceholder", "textPassword");

    @Rule
    public SystemProperty digestPassword = new SystemProperty("digestPasswordPlaceholder", "digestPassword");

    @Override
    protected String getConfigFile()
    {
        return "username-token-security-config.xml";
    }

    @Test
    public void requestWithValidCredentialsTextReturnsExpectedResult() throws Exception
    {
        assertValidResponse("vm://clientWithValidCredentialsText");
    }

    @Test
    public void requestWithValidCredentialsDigestReturnsExpectedResult() throws Exception
    {
        assertValidResponse("vm://clientWithValidCredentialsDigest");
    }

    @Test
    public void requestWithInvalidCredentialsReturnsFault() throws Exception
    {
        assertSoapFault("vm://clientWithInvalidCredentials", "FailedAuthentication");
    }

    @Test
    public void requestWithoutCredentialsReturnsFault() throws Exception
    {
        assertSoapFault("vm://clientWithoutCredentials", "InvalidSecurity");
    }


    /*
     * Mock password callback that sets the password for the user "admin".
     */
    public static class ServerPasswordCallback implements CallbackHandler
    {

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
        {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

            if (pc.getIdentifier().equals("admin"))
            {
                if (pc.getType().contains("PasswordText"))
                {
                    pc.setPassword("textPassword");
                }
                else if (pc.getType().contains("PasswordDigest"))
                {
                    pc.setPassword("digestPassword");
                }
            }
        }
    }
}
