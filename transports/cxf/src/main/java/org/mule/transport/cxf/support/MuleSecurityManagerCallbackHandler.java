/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.support;

import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.security.DefaultMuleAuthentication;
import org.mule.security.MuleCredentials;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

public class MuleSecurityManagerCallbackHandler implements CallbackHandler
{
    private org.mule.api.security.SecurityManager securityManager;
    
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
        
        if (pc.getUsage() == WSPasswordCallback.USERNAME_TOKEN
                        || pc.getUsage() == WSPasswordCallback.USERNAME_TOKEN_UNKNOWN)
        {
            DefaultMuleAuthentication auth = new DefaultMuleAuthentication(
                new MuleCredentials(pc.getIdentifer(), pc.getPassword().toCharArray()));
            
            try
            {
                securityManager.authenticate(auth);
            }
            catch (SecurityException e)
            {
                IOException ie = new IOException("password incorrect for user: " + pc.getIdentifer());
                ie.initCause(e);
                throw ie;
            }
            catch (SecurityProviderNotFoundException e)
            {
                IOException ie = new IOException("password incorrect for user: " + pc.getIdentifer());
                ie.initCause(e);
                throw ie;
            }
            
            pc.setPassword(pc.getPassword());
        }
    }

    public void setSecurityManager(org.mule.api.security.SecurityManager securityManager)
    {
        this.securityManager = securityManager;
    }

}


