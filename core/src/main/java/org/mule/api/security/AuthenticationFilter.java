/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;

/**
 * <code>AuthenticationFilter</code> is a base filter for authenticating messages.
 */
public interface AuthenticationFilter extends SecurityFilter
{
    void setCredentialsAccessor(CredentialsAccessor accessor);

    CredentialsAccessor getCredentialsAccessor();

    void authenticate(MuleEvent event)
            throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
            SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException;
}
