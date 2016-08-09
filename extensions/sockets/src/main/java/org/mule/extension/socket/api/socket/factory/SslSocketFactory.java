/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.socket.factory;

import org.mule.runtime.api.tls.TlsContextFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSocketFactory;

/**
 * Concrete implementation for {@link SimpleSocketFactory}. It provides TCP {@link Socket} that use SSL protocol.
 *
 * @since 4.0
 */
public class SslSocketFactory implements SimpleSocketFactory {

  private final SSLSocketFactory sslSocketFactory;

  public SslSocketFactory(TlsContextFactory tlsContextFactory) throws NoSuchAlgorithmException, KeyManagementException {
    sslSocketFactory = tlsContextFactory.createSslContext().getSocketFactory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket() throws IOException {
    return sslSocketFactory.createSocket();
  }
}
