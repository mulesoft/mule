/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security.provider;

import java.security.Provider;

/**
 * Determines and initializes JDK-specific security provider.
 *
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public interface SecurityProviderFactory {

    SecurityProviderInfo getSecurityProviderInfo();

    /**
     * @return an instance of a security provider
     * @throws org.mule.MuleRuntimeException if there was a problem with the security provider
     *
     * @see #getSecurityProviderInfo()
     */
    Provider getProvider();
}
