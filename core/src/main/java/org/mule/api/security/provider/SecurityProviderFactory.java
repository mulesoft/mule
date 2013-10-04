/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
