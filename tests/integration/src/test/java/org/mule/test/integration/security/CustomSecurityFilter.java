/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.security.AbstractOperationSecurityFilter;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

public class CustomSecurityFilter extends AbstractOperationSecurityFilter
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
    public void authenticate(MuleEvent event)
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
            return event.getMuleContext().getTransformationService().transform(event.getMessage(), DataTypeFactory.STRING).getPayload().equals(FunctionalTestCase.TEST_MESSAGE);
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