/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.security.tls;

import java.security.KeyStore;

import javax.net.ssl.ManagerFactoryParameters;

/**
 * Interface for certificate revocation checkers that prepare a trust manager factory configuration.
 *
 * @since 3.9
 */
public interface RevocationCheck
{

    /**
     * Configures trust store factory parameters for certificate revocation checking according to
     * the method implementation.
     *
     * @param trustStore the trust store configured for the TLS context
     * @return the configured trust manager factory parameters
     */
    ManagerFactoryParameters configFor(KeyStore trustStore);
}
