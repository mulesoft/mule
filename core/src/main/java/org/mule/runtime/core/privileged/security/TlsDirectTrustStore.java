/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.security;

import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;

import javax.net.ssl.TrustManagerFactory;

/**
 * Configure direct trust stores. TLS/SSL connections are made to trusted systems - the public certificates of trusted systems are
 * stored in a keystore (called a trust store) and used to verify that the connection made to a remote system "really is" the
 * expected identity.
 * 
 * <p>
 * The information specified in this interface may be used to configure a trust store directly, or the values in the
 * {@link TlsIndirectTrustStore} may be stored as property values and used later, or both. It may therefore be specific to a
 * single connector, or global to all connectors made by that protocol, or even (in the case of the SSL transport) become a global
 * default value. For more information see the documentation for the connector or protocol in question. The comments in
 * {@link TlsConfiguration} may also be useful.
 * </p>
 */
public interface TlsDirectTrustStore extends TlsIndirectTrustStore {

  /**
   * @return The type of keystore used to implement the trust store defined in {@link #getTrustStore()}
   */
  String getTrustStoreType();

  /**
   * @param trustStoreType The type of keystore used to implement the trust store defined in {@link #setTrustStore(String)}
   */
  void setTrustStoreType(String trustStoreType);

  /**
   * @return The algorithm used by the trust store.
   */
  String getTrustManagerAlgorithm();

  /**
   * @param trustManagerAlgorithm The algorithm used by the trust store.
   */
  void setTrustManagerAlgorithm(String trustManagerAlgorithm);

  /**
   * @return Either the factory defined by {@link #setTrustManagerFactory(TrustManagerFactory)} or one constructed from the
   *         parameters in this interface ({@link #setTrustStoreType(String)} etc).
   */
  TrustManagerFactory getTrustManagerFactory();

  /**
   * @param trustManagerFactory The source of trust information if the store is accessed directly (some connectors generate trust
   *        stores indirectly through System properties in which case this value will be ignored - see {@link TlsConfiguration}).
   */
  void setTrustManagerFactory(TrustManagerFactory trustManagerFactory);

  /**
   * If the trust store is undefined and the trust store generated via System properties then the key store certificates defined
   * via <b>TODO</b> can be used as a source of trust information.
   * 
   * @return true if the key store data should <em>not</em> be used when a trust store is otherwise undefined
   */
  boolean isExplicitTrustStoreOnly();

  /**
   * If the trust store is undefined and the trust store generated via System properties then the key store certificates defined
   * via <b>TODO</b> can be used as a source of trust information.
   * 
   * @param explicitTrustStoreOnly true if the key store data should <em>not<em> be used when a trust store is otherwise undefined
   */
  void setExplicitTrustStoreOnly(boolean explicitTrustStoreOnly);

  /**
   * If a server socket is constructed directly (see {@link TlsConfiguration}) then this flag will control whether client
   * authenticatin is required. This does not apply to client connections.
   * 
   * @return true if clients must be authenticated
   */
  boolean isRequireClientAuthentication();

  /**
   * If a server socket is constructed directly (see {@link TlsConfiguration}) then this flag will control whether client
   * authenticatin is required. This does not apply to client connections.
   * 
   * @param requireClientAuthentication true if clients must be authenticated
   */
  void setRequireClientAuthentication(boolean requireClientAuthentication);

}


