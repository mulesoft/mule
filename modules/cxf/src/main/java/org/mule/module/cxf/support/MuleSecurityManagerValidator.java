/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
