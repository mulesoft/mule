/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.security;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.security.AbstractAuthenticationFilter;

public class TestSecurityFilter extends AbstractAuthenticationFilter
{
    private boolean accept;
    private boolean called;

    public static final String SECURITY_EXCEPTION_MESSAGE = "unauthorized!!";

    public TestSecurityFilter(boolean accept)
    {
        this.accept = accept;
    }

    @Override
    public void authenticate(MuleEvent event)
        throws SecurityException, CryptoFailureException, SecurityProviderNotFoundException,
        EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException
    {
        called = true;
        if (!accept)
        {
            throw new StaticMessageUnauthorisedException();
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
    }

    public boolean wasCalled()
    {
        return called;
    }

    public static class StaticMessageUnauthorisedException extends UnauthorisedException
    {
        public StaticMessageUnauthorisedException()
        {
            super(null);
        }

        @Override
        public String getLocalizedMessage()
        {
            return SECURITY_EXCEPTION_MESSAGE;
        }
    }

}
