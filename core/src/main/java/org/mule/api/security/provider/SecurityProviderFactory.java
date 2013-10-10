/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security.provider;

import java.security.Provider;

/**
 * Determines and initializes JDK-specific security provider.
 */
public interface SecurityProviderFactory
{

    SecurityProviderInfo getSecurityProviderInfo();

    /**
     * @return an instance of a security provider
     * @throws org.mule.api.MuleRuntimeException if there was a problem with the security
     *             provider
     * @see #getSecurityProviderInfo()
     */
    Provider getProvider();
}
