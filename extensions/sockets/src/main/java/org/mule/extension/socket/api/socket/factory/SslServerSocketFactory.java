/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.socket.factory;

import org.mule.runtime.api.tls.TlsContextFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Concrete implementation for {@link SimpleServerSocketFactory}. It provides TCP {@link ServerSocket} that use SSL protocol.
 *
 * @since 4.0
 */
public class SslServerSocketFactory implements SimpleServerSocketFactory {

  private final TlsContextFactory tlsContextFactory;
  private final SSLServerSocketFactory sslServerSocketFactory;

  public SslServerSocketFactory(TlsContextFactory tlsContextFactory) throws NoSuchAlgorithmException, KeyManagementException {
    this.tlsContextFactory = tlsContextFactory;
    sslServerSocketFactory = tlsContextFactory.createSslContext().getServerSocketFactory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServerSocket createServerSocket() throws IOException {
    SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket();
    sslServerSocket.setNeedClientAuth(tlsContextFactory.isTrustStoreConfigured());
    return sslServerSocket;
  }
}
