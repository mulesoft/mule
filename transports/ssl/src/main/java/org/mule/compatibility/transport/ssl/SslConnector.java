/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import org.mule.compatibility.transport.tcp.TcpConnector;
import org.mule.compatibility.transport.tcp.protocols.DirectProtocol;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.TlsDirectKeyStore;
import org.mule.runtime.core.api.security.TlsDirectTrustStore;
import org.mule.runtime.core.api.security.TlsIndirectKeyStore;
import org.mule.runtime.core.api.security.tls.TlsConfiguration;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;

/**
 * <code>SslConnector</code> provides a connector for SSL connections. Note that the *only* function of the code in this package
 * is to configure and provide SSL enabled sockets. All other logic is identical to TCP.
 */
public class SslConnector extends TcpConnector implements TlsDirectKeyStore, TlsIndirectKeyStore, TlsDirectTrustStore {

  public static final String SSL = "ssl";
  public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
  public static final String LOCAL_CERTIFICATES = "LOCAL_CERTIFICATES";

  // null initial keystore - see below
  private TlsConfiguration tls = new TlsConfiguration(null);

  /**
   * Timeout for establishing the SSL connection with the client.
   */
  private long sslHandshakeTimeout = 30000;

  public SslConnector(MuleContext context) {
    super(context);
    setSocketFactory(new SslSocketFactory(tls));
    setServerSocketFactory(new SslServerSocketFactory(tls));
    setTcpProtocol(new DirectProtocol());
    // setting this true causes problems as socket closes before handshake finishes
    setValidateConnections(false);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    super.doInitialise();
    // the original logic here was slightly different to other uses of the TlsSupport code -
    // it appeared to be equivalent to switching anon by whether or not a keyStore was defined
    // (which seems to make sense), so that is used here.
    try {
      tls.initialise(null == getKeyStore(), TlsConfiguration.JSSE_NAMESPACE);
    } catch (CreateException e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  protected ServerSocket getServerSocket(URI uri) throws IOException {
    SSLServerSocket serverSocket = (SSLServerSocket) super.getServerSocket(uri);
    serverSocket.setNeedClientAuth(isRequireClientAuthentication());
    return serverSocket;
  }

  @Override
  public String getProtocol() {
    return SSL;
  }

  @Override
  public String getClientKeyStore() {
    return tls.getClientKeyStore();
  }

  @Override
  public String getClientKeyStorePassword() {
    return tls.getClientKeyStorePassword();
  }

  @Override
  public String getClientKeyStoreType() {
    return this.tls.getClientKeyStoreType();
  }

  @Override
  public String getKeyManagerAlgorithm() {
    return tls.getKeyManagerAlgorithm();
  }

  @Override
  public KeyManagerFactory getKeyManagerFactory() {
    return tls.getKeyManagerFactory();
  }

  @Override
  public String getKeyPassword() {
    return tls.getKeyPassword();
  }

  @Override
  public String getKeyStore() {
    return tls.getKeyStore();
  }

  @Override
  public String getKeyAlias() {
    return tls.getKeyAlias();
  }

  @Override
  public String getKeyStoreType() {
    return tls.getKeyStoreType();
  }

  public String getSslType() {
    return tls.getSslType();
  }

  @Override
  public String getKeyStorePassword() {
    return tls.getKeyStorePassword();
  }

  @Override
  public String getTrustManagerAlgorithm() {
    return tls.getTrustManagerAlgorithm();
  }

  @Override
  public TrustManagerFactory getTrustManagerFactory() {
    return tls.getTrustManagerFactory();
  }

  @Override
  public String getTrustStore() {
    return tls.getTrustStore();
  }

  @Override
  public String getTrustStorePassword() {
    return tls.getTrustStorePassword();
  }

  @Override
  public String getTrustStoreType() {
    return tls.getTrustStoreType();
  }

  @Override
  public boolean isExplicitTrustStoreOnly() {
    return tls.isExplicitTrustStoreOnly();
  }

  @Override
  public boolean isRequireClientAuthentication() {
    return tls.isRequireClientAuthentication();
  }

  @Override
  public void setClientKeyStore(String clientKeyStore) throws IOException {
    tls.setClientKeyStore(clientKeyStore);
  }

  @Override
  public void setClientKeyStorePassword(String clientKeyStorePassword) {
    tls.setClientKeyStorePassword(clientKeyStorePassword);
  }

  @Override
  public void setClientKeyStoreType(String clientKeyStoreType) {
    this.tls.setClientKeyStoreType(clientKeyStoreType);
  }

  @Override
  public void setExplicitTrustStoreOnly(boolean explicitTrustStoreOnly) {
    tls.setExplicitTrustStoreOnly(explicitTrustStoreOnly);
  }

  @Override
  public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
    tls.setKeyManagerAlgorithm(keyManagerAlgorithm);
  }

  @Override
  public void setKeyPassword(String keyPassword) {
    tls.setKeyPassword(keyPassword);
  }

  @Override
  public void setKeyStore(String keyStore) throws IOException {
    tls.setKeyStore(keyStore);
  }

  @Override
  public void setKeyAlias(String alias) {
    tls.setKeyAlias(alias);
  }

  @Override
  public void setKeyStoreType(String keystoreType) {
    tls.setKeyStoreType(keystoreType);
  }

  @Override
  public void setRequireClientAuthentication(boolean requireClientAuthentication) {
    tls.setRequireClientAuthentication(requireClientAuthentication);
  }

  public void setSslType(String sslType) {
    tls.setSslType(sslType);
  }

  @Override
  public void setKeyStorePassword(String storePassword) {
    tls.setKeyStorePassword(storePassword);
  }

  @Override
  public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
    tls.setTrustManagerAlgorithm(trustManagerAlgorithm);
  }

  @Override
  public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
    tls.setTrustManagerFactory(trustManagerFactory);
  }

  @Override
  public void setTrustStore(String trustStore) throws IOException {
    tls.setTrustStore(trustStore);
  }

  @Override
  public void setTrustStorePassword(String trustStorePassword) {
    tls.setTrustStorePassword(trustStorePassword);
  }

  @Override
  public void setTrustStoreType(String trustStoreType) {
    tls.setTrustStoreType(trustStoreType);
  }

  public long getSslHandshakeTimeout() {
    return sslHandshakeTimeout;
  }

  public void setSslHandshakeTimeout(long sslHandshakeTimeout) {
    this.sslHandshakeTimeout = sslHandshakeTimeout;
  }

}
