/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import org.mule.compatibility.transport.ssl.SslServerSocketFactory;
import org.mule.compatibility.transport.ssl.SslSocketFactory;
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
import java.security.GeneralSecurityException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * <code>HttpsConnector</code> provides Secure http connectivity on top of what is already provided with the Mule
 * {@link org.mule.compatibility.transport.http.HttpConnector}.
 */
public class HttpsConnector extends HttpConnector implements TlsDirectKeyStore, TlsIndirectKeyStore, TlsDirectTrustStore {

  public static final String HTTPS = "https";
  public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
  public static final String LOCAL_CERTIFICATES = "LOCAL_CERTIFICATES";

  // null initial keystore - see below
  private TlsConfiguration tls = new TlsConfiguration(null);

  /**
   * Timeout for establishing the SSL connection with the client.
   */
  private long sslHandshakeTimeout = 30000;

  public HttpsConnector(MuleContext context) {
    super(context);
    setSocketFactory(new SslSocketFactory(tls));
    setServerSocketFactory(new SslServerSocketFactory(tls));
    // setting this true causes problems as socket closes before handshake finishes
    setValidateConnections(false);
  }

  @Override
  protected ServerSocket getServerSocket(URI uri) throws IOException {
    SSLServerSocket serverSocket = (SSLServerSocket) super.getServerSocket(uri);
    serverSocket.setNeedClientAuth(isRequireClientAuthentication());
    return serverSocket;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    // if a keystore is not provided, the connector will only be used for
    // client connections, and can work in anon mode.
    try {
      tls.initialise(null == getKeyStore(), TlsConfiguration.JSSE_NAMESPACE);
    } catch (CreateException e) {
      throw new InitialisationException(e, this);
    }
    super.doInitialise();
  }

  @Override
  public String getProtocol() {
    return HTTPS;
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
  public String getKeyAlias() {
    return tls.getKeyAlias();
  }

  @Override
  public String getKeyStore() {
    return tls.getKeyStore();
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
  public void setKeyAlias(String keyAlias) {
    tls.setKeyAlias(keyAlias);
  }

  @Override
  public void setKeyStore(String keyStore) throws IOException {
    tls.setKeyStore(keyStore);
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

  public SSLSocketFactory getSslSocketFactory() throws GeneralSecurityException {
    return tls.getSocketFactory();
  }

}
