/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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

    Collection getProviders();

    void setProviders(Collection providers);

    SecurityContext createSecurityContext(Authentication authentication)
        throws UnknownAuthenticationTypeException;

    EncryptionStrategy getEncryptionStrategy(String name);

    void addEncryptionStrategy(EncryptionStrategy strategy);

    EncryptionStrategy removeEncryptionStrategy(String name);

    Collection getEncryptionStrategies();

    void setEncryptionStrategies(Collection strategies);

}
