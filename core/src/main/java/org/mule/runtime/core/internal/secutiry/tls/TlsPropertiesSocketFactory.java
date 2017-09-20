/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.secutiry.tls;

import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A socket factory that is configured via Properties, using a {@link TlsConfiguration} that has been stored via
 * {@link TlsPropertiesMapper}.
 */
public class TlsPropertiesSocketFactory extends SSLSocketFactory {

  private Logger logger = LoggerFactory.getLogger(getClass());
  private boolean anon;
  private String namespace;
  private SSLSocketFactory factory;

  public TlsPropertiesSocketFactory(boolean anon, String namespace) {
    super();
    logger.debug("creating: " + anon + "; " + namespace);
    this.anon = anon;
    this.namespace = namespace;
  }

  private synchronized SSLSocketFactory getFactory() throws IOException {
    if (null == factory) {
      logger.debug("creating factory");
      TlsPropertiesMapper propertiesMapper = new TlsPropertiesMapper(namespace);
      TlsConfiguration configuration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
      propertiesMapper.readFromProperties(configuration, System.getProperties());
      try {
        configuration.initialise(anon, namespace);
        factory = configuration.getSocketFactory();
      } catch (Exception e) {
        throw (IOException) new IOException(e.getMessage()).initCause(e);
      }
    }
    return factory;
  }

  @Override
  public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    return getFactory().createSocket(s, host, port, autoClose);
  }

  @Override
  public String[] getDefaultCipherSuites() {
    try {
      return getFactory().getDefaultCipherSuites();
    } catch (Exception e) {
      return new String[0];
    }
  }

  @Override
  public String[] getSupportedCipherSuites() {
    try {
      return getFactory().getSupportedCipherSuites();
    } catch (Exception e) {
      return new String[0];
    }
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return getFactory().createSocket(host, port);
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    return getFactory().createSocket(host, port);
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
    return getFactory().createSocket(host, port);
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
    return getFactory().createSocket(address, port, localAddress, localPort);
  }

  // see http://forum.java.sun.com/thread.jspa?threadID=701799&messageID=4280973
  @Override
  public Socket createSocket() throws IOException {
    return getFactory().createSocket();
  }

}


