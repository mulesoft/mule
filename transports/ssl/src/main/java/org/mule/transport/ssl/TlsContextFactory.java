/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

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

    public SSLSocketFactory getSocketFactory() throws NoSuchAlgorithmException, KeyManagementException;

    public SSLServerSocketFactory getServerSocketFactory() throws NoSuchAlgorithmException, KeyManagementException;

}
