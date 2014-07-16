/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import org.mule.api.EncryptionStrategy;
import org.mule.api.lifecycle.Initialisable;

import java.util.Collection;

/**
 * <code>SecurityManager</code> is responsible for managing one or more
 * security providers.
 */

public interface SecurityManager extends Initialisable
{
    
    Authentication authenticate(Authentication authentication)
        throws SecurityException, SecurityProviderNotFoundException;

    void addProvider(SecurityProvider provider);

    SecurityProvider getProvider(String name);

    SecurityProvider removeProvider(String name);

    Collection<SecurityProvider> getProviders();

    void setProviders(Collection<SecurityProvider> providers);

    SecurityContext createSecurityContext(Authentication authentication)
        throws UnknownAuthenticationTypeException;

    EncryptionStrategy getEncryptionStrategy(String name);

    void addEncryptionStrategy(EncryptionStrategy strategy);

    EncryptionStrategy removeEncryptionStrategy(String name);

    Collection<EncryptionStrategy> getEncryptionStrategies();

    void setEncryptionStrategies(Collection<EncryptionStrategy> strategies);

}
