/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.handler;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

public class TestCallbackHandler implements CallbackHandler
{
    private static final String PASSWORD = "password";

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        for (int i = 0; i < callbacks.length; i++)
        {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
            pc.setPassword(PASSWORD);
        }
    }
}
