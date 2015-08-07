/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.functional;

import static org.apache.ws.security.WSPasswordCallback.DECRYPT;
import static org.apache.ws.security.WSPasswordCallback.SIGNATURE;
import static org.apache.ws.security.WSPasswordCallback.USERNAME_TOKEN;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;
import org.junit.Test;


public class CombinedSecurityFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "combined-security-config.xml";
    }

    @Test
    public void validRequestReturnsExpectedResult() throws Exception
    {
        assertValidResponse("vm://request");
    }


    public static class ServerPasswordCallback implements CallbackHandler
    {

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
        {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

            if (pc.getUsage() == USERNAME_TOKEN)
            {
                pc.setPassword("textPassword");
            }
            else if (pc.getUsage() == SIGNATURE)
            {
                pc.setPassword("mulepassword");
            }
            else if (pc.getUsage() == DECRYPT)
            {
                pc.setPassword("changeit");
            }
        }
    }
}
