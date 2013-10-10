/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.RequestContext;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.security.DefaultMuleAuthentication;
import org.mule.security.MuleCredentials;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityException;

public class MuleSecurityManagerCallbackHandler implements CallbackHandler
{
    private static Log logger = LogFactory.getLog(MuleSecurityManagerCallbackHandler.class);
    
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
                Authentication authentication = securityManager.authenticate(auth);
                pc.setPassword(pc.getPassword());

                SecurityContext secContext = null;
                try
                {
                    secContext = securityManager.createSecurityContext(authentication);
                    secContext.setAuthentication(authentication);
                }
                catch (UnknownAuthenticationTypeException e)
                {
                    logger.warn("Could not create security context after having successfully authenticated.", e);
                }
                RequestContext.getEvent().getSession().setSecurityContext(secContext);
            }
            catch (SecurityException e)
            {
                throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, null, null, e);
            }
            catch (SecurityProviderNotFoundException e)
            {
                throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, null, null, e);
            }
        }
    }

    public void setSecurityManager(org.mule.api.security.SecurityManager securityManager)
    {
        this.securityManager = securityManager;
    }

}


