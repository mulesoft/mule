/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
