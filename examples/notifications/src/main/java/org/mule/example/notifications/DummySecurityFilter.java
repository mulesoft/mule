/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.notifications;

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

import java.util.Map;

/**
 * TODO
 */
public class DummySecurityFilter extends AbstractEndpointSecurityFilter
{
    protected void authenticateInbound(MuleEvent event) throws org.mule.api.security.SecurityException, CryptoFailureException, SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException
    {
        doAuthenticate(event);
    }

    protected void authenticateOutbound(MuleEvent event) throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException
    {
        doAuthenticate(event);
    }

    protected void doAuthenticate(MuleEvent event) throws UnauthorisedException
    {
        try
        {
            Map payload = (Map) event.getMessage().getPayload(Map.class);
            String user = (String) payload.get("user");
            if (user == null)
            {
                throw new UnauthorisedException(CoreMessages.authNoCredentials());
            }
            if ("anonymous".equals(user))
            {
                throw new UnauthorisedException(CoreMessages.authFailedForUser("anonymous"));
            }
        }
        catch (Exception e)
        {
            throw new UnauthorisedException(CoreMessages.authFailedForUser("anonymous"), e);
        }

    }

    protected void doInitialise() throws InitialisationException
    {

    }
}
