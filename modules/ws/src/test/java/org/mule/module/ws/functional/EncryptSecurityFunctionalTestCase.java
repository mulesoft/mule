/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.functional;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;
import org.junit.Test;


public class EncryptSecurityFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "encrypt-security-config.xml";
    }

    @Test
    public void requestEncryptedWithValidKeyReturnsExpectedResult() throws Exception
    {
        assertValidResponse("vm://requestEncryptedWithValidKey");
    }

    @Test
    public void requestEncryptedWithInvalidKeyFails() throws Exception
    {
        assertSoapFault("vm://requestEncryptedWithInvalidKey", "Client");
    }

    @Test
    public void requestNotEncryptedFails() throws Exception
    {
        assertSoapFault("vm://requestNotEncrypted", "InvalidSecurity");
    }


    public static class ServerPasswordCallback implements CallbackHandler
    {

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
        {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
            pc.setPassword("changeit");
        }
    }

}
