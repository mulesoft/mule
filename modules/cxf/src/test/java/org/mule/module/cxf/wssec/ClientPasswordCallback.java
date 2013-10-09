/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.wssec;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

/**
 * This callback simply supplies the password so that it's not stored in our config file.
 * You will need to call 
 *   ClientPasswordCallback.setPassword("password"); 
 * from your code before invoking the secure service.
 */
public class ClientPasswordCallback implements CallbackHandler
{
    private static String password;
    
    public static void setPassword(String password)
    {
        ClientPasswordCallback.password = password;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException 
    {
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        // set the password for our message.
        pc.setPassword(password);
    }
}


