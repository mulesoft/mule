/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

/**
 * <code>SecurityFilter</code> is a base filter for secure filtering of
 * inbound and outbound event flow.
 */
public interface SecurityFilter extends Initialisable
{
    /**
     * @param manager
     */
    void setSecurityManager(SecurityManager manager);

    SecurityManager getSecurityManager();

    String getSecurityProviders();

    void setSecurityProviders(String providers);
    
    void doFilter(MuleEvent event)
            throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
            SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException;
}
