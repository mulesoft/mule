/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jaas.filters;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.Credentials;
import org.mule.api.security.CredentialsNotSetException;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.jaas.JaasAuthentication;
import org.mule.security.AbstractEndpointSecurityFilter;
import org.mule.security.MuleCredentials;
import org.mule.security.MuleHeaderCredentialsAccessor;

public class JaasSecurityFilter extends AbstractEndpointSecurityFilter
{

    public JaasSecurityFilter()
    {
        setCredentialsAccessor(new MuleHeaderCredentialsAccessor());
    }

    @Override
    protected final void authenticateInbound(MuleEvent event)
        throws SecurityException, CryptoFailureException, EncryptionStrategyNotFoundException,
        UnknownAuthenticationTypeException
    {
        String userHeader = (String) getCredentialsAccessor().getCredentials(event);
        if (userHeader == null)
        {
            throw new CredentialsNotSetException(event, event.getSession().getSecurityContext(), this);
        }

        Credentials user = new MuleCredentials(userHeader, getSecurityManager());
        Authentication authResult;
        JaasAuthentication authentication = new JaasAuthentication(user);
        authentication.setEvent(event);
        try
        {
            authResult = getSecurityManager().authenticate(authentication);
        }
        catch (SecurityException se)
        {
            // Security Exception occurred
            if (logger.isDebugEnabled())
            {
                logger.debug("Security Exception raised. Authentication request for user: " + user.getUsername()
                    + " failed: " + se.toString());
            }
            throw se;
        }
        catch (Exception e)
        {
            // Authentication failed
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication request for user: " + user.getUsername()
                    + " failed: " + e.toString());
            }
            throw new UnauthorisedException(
                CoreMessages.authFailedForUser(user.getUsername()), event, e);
        }

        // Authentication success
        if (logger.isDebugEnabled())
        {
            logger.debug("Authentication success: " + authResult.toString());
        }

        SecurityContext context = getSecurityManager().createSecurityContext(authResult);
        context.setAuthentication(authResult);
        event.getSession().setSecurityContext(context);
    }

    @Override
    protected void authenticateOutbound(MuleEvent event)
        throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException
    {
        SecurityContext securityContext = event.getSession().getSecurityContext();
        if (securityContext == null)
        {
            if (isAuthenticate())
            {
                throw new UnauthorisedException(event, securityContext, this);
            }
            else
            {
                return;
            }
        }

        Authentication auth = securityContext.getAuthentication();
        if (isAuthenticate())
        {
            auth = getSecurityManager().authenticate(auth);
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication success: " + auth.toString());
            }
        }

        String token = auth.getCredentials().toString();
        getCredentialsAccessor().setCredentials(event, token);

    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        // empty constructor
    }
}

