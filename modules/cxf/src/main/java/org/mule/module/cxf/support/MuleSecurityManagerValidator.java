/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.RequestContext;
import org.mule.api.security.*;
import org.mule.security.DefaultMuleAuthentication;
import org.mule.security.MuleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;

/**
 *  Integrates mule spring security with CXF ws-security
 */
public class MuleSecurityManagerValidator implements Validator
{
    private static Log logger = LogFactory.getLog(MuleSecurityManagerValidator.class);
    
    private org.mule.api.security.SecurityManager securityManager;

    public Credential validate(Credential credential, RequestData data) throws WSSecurityException
    {
        if (credential == null || credential.getUsernametoken() == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "noCredential");
        }

        DefaultMuleAuthentication auth = new DefaultMuleAuthentication(
            new MuleCredentials(credential.getUsernametoken().getName(), credential.getUsernametoken().getPassword().toCharArray()));

        try
        {
          Authentication authentication = securityManager.authenticate(auth);

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
        catch (org.mule.api.security.SecurityException e)
        {
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, null, null, e);
        }
        catch (SecurityProviderNotFoundException e)
        {
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, null, null, e);
        }

        return credential;
    }

    public void setSecurityManager(org.mule.api.security.SecurityManager securityManager)
    {
        this.securityManager = securityManager;
    }

}
