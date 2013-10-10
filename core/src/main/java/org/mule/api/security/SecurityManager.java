/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
