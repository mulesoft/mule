/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.security;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.AbstractEndpointSecurityFilter;
import org.mule.tck.FunctionalTestCase;

public class CustomSecurityFilter extends AbstractEndpointSecurityFilter
{

    public CustomSecurityFilter()
    {

    }

    @Override
    protected void authenticateInbound(MuleEvent event)
        throws SecurityException, CryptoFailureException, SecurityProviderNotFoundException,
        EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException
    {
        if (!isValid(event))
        {
            throw new UnauthorisedException(CoreMessages.authFailedForUser("a"));
        }
    }

    @Override
    protected void authenticateOutbound(MuleEvent event)
        throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException
    {
        if (!isValid(event))
        {
            throw new UnauthorisedException(CoreMessages.authFailedForUser("a"));
        }
    }

    private boolean isValid(MuleEvent event)
    {
        try
        {
            return event.getMessage().getPayloadAsString().equals(FunctionalTestCase.TEST_MESSAGE);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
    }

}