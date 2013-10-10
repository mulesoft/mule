/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import org.mule.api.security.Authentication;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class MuleCallbackHandler implements CallbackHandler
{
    private Authentication authentication;
    private String username;
    private String password;

    /**
     * @param authentication
     */
    public MuleCallbackHandler(Authentication authentication)
    {
        this.authentication = authentication;
        this.username = (String) this.authentication.getPrincipal();
        this.password = (String) this.authentication.getCredentials();
    }

    /**
     * The handle() method handles the callbacks to be passed to the Jaas security.
     * It makes use of two types of callbacks: the NameCallback and the
     * PasswordCallback.
     * 
     * @param callbacks
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    public final void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        for (int i = 0; i < callbacks.length; i++)
        {
            if (callbacks[i] instanceof NameCallback)
            {
                NameCallback nameCb = (NameCallback) callbacks[i];
                nameCb.setName(username);
            }
            else if (callbacks[i] instanceof PasswordCallback)
            {
                PasswordCallback passCb = (PasswordCallback) callbacks[i];
                passCb.setPassword(password.toCharArray());
            }
            else
            {
                throw (new UnsupportedCallbackException(callbacks[i], "Callback class not supported"));
            }
        }
    }
}
