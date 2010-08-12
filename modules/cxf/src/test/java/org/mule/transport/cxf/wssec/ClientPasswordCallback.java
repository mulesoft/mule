/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.wssec;

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


