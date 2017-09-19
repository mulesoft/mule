/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.secutiry.tls;

import org.mule.runtime.core.internal.util.ArrayUtils;

import com.google.common.base.Joiner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SSLServerSocketFactory decorator that restricts the available protocols and cipher suites in the sockets that are created.
 */
public class RestrictedSSLServerSocketFactory extends SSLServerSocketFactory {

  private static final Logger logger = LoggerFactory.getLogger(RestrictedSSLServerSocketFactory.class);

  private final SSLServerSocketFactory sslServerSocketFactory;
  private final String[] enabledCipherSuites;
  private final String[] enabledProtocols;
  private final String[] defaultCipherSuites;

  public RestrictedSSLServerSocketFactory(SSLContext sslContext, String[] cipherSuites, String[] protocols) {
    this.sslServerSocketFactory = sslContext.getServerSocketFactory();

    if (cipherSuites == null) {
      cipherSuites = sslServerSocketFactory.getDefaultCipherSuites();
    }
    this.enabledCipherSuites = ArrayUtils.intersection(cipherSuites, sslServerSocketFactory.getSupportedCipherSuites());
    this.defaultCipherSuites = ArrayUtils.intersection(cipherSuites, sslServerSocketFactory.getDefaultCipherSuites());

    if (protocols == null) {
      protocols = sslContext.getDefaultSSLParameters().getProtocols();
    }
    this.enabledProtocols = ArrayUtils.intersection(protocols, sslContext.getSupportedSSLParameters().getProtocols());

    if (this.enabledProtocols.length != protocols.length) {
      logger.warn("Current context supports less SSL protocols than configured. Only the following are enabled: [{}]",
                  Joiner.on(", ").join(this.enabledProtocols));
    }
  }

  @Override
  public ServerSocket createServerSocket() throws IOException {
    return restrictCipherSuites((SSLServerSocket) sslServerSocketFactory.createServerSocket());
  }

  @Override
  public ServerSocket createServerSocket(int port) throws IOException {
    return restrictCipherSuites((SSLServerSocket) sslServerSocketFactory.createServerSocket(port));
  }

  @Override
  public ServerSocket createServerSocket(int port, int backlog) throws IOException {
    return restrictCipherSuites((SSLServerSocket) sslServerSocketFactory.createServerSocket(port, backlog));
  }

  @Override
  public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
    return restrictCipherSuites((SSLServerSocket) sslServerSocketFactory.createServerSocket(port, backlog, ifAddress));
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return defaultCipherSuites;
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return enabledCipherSuites;
  }

  private SSLServerSocket restrictCipherSuites(SSLServerSocket sslServerSocket) {
    sslServerSocket.setEnabledCipherSuites(enabledCipherSuites);
    sslServerSocket.setEnabledProtocols(enabledProtocols);
    return sslServerSocket;
  }
}
