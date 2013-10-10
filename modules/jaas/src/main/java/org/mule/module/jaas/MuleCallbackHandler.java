/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
