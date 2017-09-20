/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.security;

import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;

import java.io.IOException;

import javax.net.ssl.KeyManagerFactory;

/**
 * Configure direct key stores. TLS/SSL connections are made on behalf of an entity, which can be anonymous or identified by a
 * certificate - this interface specifies how a keystore can be used to provide the certificates (and associated private keys)
 * necessary for identification.
 *
 * <p>
 * The information specified in this interface is used to configure a key store directly. For more information see the
 * documentation for the connector or protocol in question. The comments in
 * {@link TlsConfiguration} may also be useful.
 * </p>
 */
public interface TlsDirectKeyStore {

  /**
   * @return The location (resolved relative to the current classpath and file system, if possible) of the keystore that contains
   *         public certificates and private keys for identification.
   */
  String getKeyStore();

  /**
   * @param name The location of the keystore that contains public certificates and private keys for identification.
   * @throws IOException If the location cannot be resolved via the file system or classpath
   */
  void setKeyStore(String name) throws IOException;

  /**
   * @return The alias of the key from the key store.
   */
  String getKeyAlias();

  /**
   * @param alias of the key from the key store.
   */
  void setKeyAlias(String alias);

  /**
   * @return The password used to protect the private key(s)
   */
  String getKeyPassword();

  /**
   * @param keyPassword The password used to protect the private key(s)
   */
  void setKeyPassword(String keyPassword);

  /**
   * @return The password used to protect the keystore itself
   */
  String getKeyStorePassword();

  /**
   * @param storePassword The password used to protect the keystore itself
   */
  void setKeyStorePassword(String storePassword);

  /**
   * @return The type of keystore used in {@link #getKeyStore()}
   */
  String getKeyStoreType();

  /**
   * @param keystoreType The type of keystore used in {@link #setKeyStore(String)}
   */
  void setKeyStoreType(String keystoreType);

  /**
   * @return The algorithm used by the key store. The default comes from {
   * @link org.mule.runtime.core.api.security.provider.AutoDiscoverySecurityProviderFactory}
   */
  String getKeyManagerAlgorithm();

  /**
   * @param keyManagerAlgorithm The algorithm used by the key store. The default comes from {
   * @link org.mule.runtime.core.api.security.provider.AutoDiscoverySecurityProviderFactory}
   */
  void setKeyManagerAlgorithm(String keyManagerAlgorithm);

  /**
   * @return A source of key stores generated from the parameters supplied here.
   */
  KeyManagerFactory getKeyManagerFactory();
}


