/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.NotPermittedException;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.spring.security.i18n.SpringSecurityMessages;
import org.mule.security.AbstractSecurityFilter;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authorizes user access based on the required authorities for a user.
 */
public class AuthorizationFilter extends AbstractSecurityFilter
{
    protected final Log logger = LogFactory.getLog(getClass());
    private Collection<String> requiredAuthorities = new HashSet<String>();

    public void doFilter(MuleEvent event)
        throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
        SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException
    {
        Authentication auth = event.getSession().getSecurityContext().getAuthentication();
        if (auth == null)
        {
            throw new UnauthorisedException(CoreMessages.authNoCredentials());
        }

        if (!(auth instanceof SpringAuthenticationAdapter))
        {
            throw new UnauthorisedException(SpringSecurityMessages.springAuthenticationRequired());
        }

        SpringAuthenticationAdapter springAuth = (SpringAuthenticationAdapter) auth;

        String principalName = springAuth.getName();
        GrantedAuthority[] authorities = springAuth.getAuthorities();

        // If the principal has at least one of the granted authorities,
        // then return.
        boolean authorized = false;
        if (authorities != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Found authorities '" + Arrays.toString(authorities) + "' for principal '"
                             + principalName + "'.");
            }

            for (GrantedAuthority authority : authorities)
            {
                if (requiredAuthorities.contains(authority.getAuthority()))
                {
                    authorized = true;
                }
            }
        }

        if (!authorized)
        {
            logger.info(MessageFormat.format("Could not find required authorities for {0}. Required authorities: {1}. Authorities found: {2}.", 
                principalName, Arrays.toString(requiredAuthorities.toArray()), Arrays.toString(authorities)));
            throw new NotPermittedException(SpringSecurityMessages.noGrantedAuthority(principalName));
        }
    }

    public Collection<String> getRequiredAuthorities()
    {
        return requiredAuthorities;
    }

    public void setRequiredAuthorities(Collection<String> requiredAuthorities)
    {
        this.requiredAuthorities = requiredAuthorities;
    }
}
