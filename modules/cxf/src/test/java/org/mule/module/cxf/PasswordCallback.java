/*
 * $Id: PasswordCallback.java 18408 2010-12-09 19:44:58Z travis.carlson $
 * --------------------------------------------------------------------------------------
 *
 * (c) 2003-2010 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */

package org.mule.module.cxf;

import org.apache.ws.security.WSPasswordCallback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

//import org.apache.ws.security.WSPasswordCallback;

public class PasswordCallback implements CallbackHandler
{
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        if (pc.getIdentifier().equals("joe"))
        {
            pc.setPassword("secret");
        }
        else if (pc.getIdentifier().equals("stan"))
        {
            pc.setPassword("elephant");
        }
    }
}



