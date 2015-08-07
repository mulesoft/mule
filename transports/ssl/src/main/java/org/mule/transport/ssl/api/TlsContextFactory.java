/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.api;

import org.mule.api.lifecycle.CreateException;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

/**
 * A factory for TLS contexts. A TLS context is configured with a key store and a trust store. The key store stores
 * the private and public keys of the current host. The trust store stores the certificates of the other hosts that are
 * trusted.
 *
 * This factory provides methods for creating client and server socket factories that will be already configured
 * according to the current context.
 */
public interface TlsContextFactory
{

    /**
     * @return a new SSL Context with the configured keystore and trustore.
     *
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws CreateException
     */
    SSLContext createSslContext() throws KeyManagementException, NoSuchAlgorithmException, CreateException;

    /**
     * The list of ciphers that must be used to restrict the creation of the SSL Sockets
     *
     * @return ths list of enabled cipher suites
     */
    String[] getEnabledCipherSuites();

    /**
     * The list of enabled protocols that must be used to restrict the creation of the SSL Sockets
     *
     * @return the list of enabled protocols
     */
    String[] getEnabledProtocols();

    /**
     * @return true if the keystore was configured, false otherwise
     */
    boolean isKeyStoreConfigured();

    /**
     * @return true if the trust store was configured, false otherwise
     */
    boolean isTrustStoreConfigured();

    /**
     * @return An object with the configuration of the key store.
     */
    TlsContextKeyStoreConfiguration getKeyStoreConfiguration();

    /**
     * @return An object with the configuration of the trust store.
     */
    TlsContextTrustStoreConfiguration getTrustStoreConfiguration();
}
