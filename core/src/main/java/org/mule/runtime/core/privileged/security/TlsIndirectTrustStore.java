/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.security;

import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;

import java.io.IOException;

/**
 * Configure indirect trust stores. TLS/SSL connections are made to trusted systems - the public certificates of trusted systems
 * are store in a keystore (called a trust store) and used to verify that the connection made to a remote system "really is" the
 * expected identity.
 * 
 * <p>
 * The information specified in this interface may be used to configure a trust store directly, as part of
 * {@link TlsDirectKeyStore}, or it may be stored as property values and used later, or both. It may therefore be specific to a
 * single connector, or global to all connectors made by that protocol, or even (in the case of the SSL transport) become a global
 * default value. For more information see the documentation for the connector or protocol in question. The comments in
 * {@link TlsConfiguration} may also be useful.
 * </p>
 */
public interface TlsIndirectTrustStore {

  /**
   * @return The location (resolved relative to the current classpath and file system, if possible) of the keystore that contains
   *         public certificates of trusted servers.
   */
  String getTrustStore();

  /**
   * @param name The location of the keystore that contains public certificates of trusted servers.
   * @throws IOException If the location cannot be resolved via the file system or classpath
   */
  void setTrustStore(String name) throws IOException;

  /**
   * @return The password used to protected the trust store defined in {@link #getTrustStore()}
   */
  String getTrustStorePassword();

  /**
   * @param trustStorePassword The password used to protected the trust store defined in {@link #setTrustStore(String)}
   */
  void setTrustStorePassword(String trustStorePassword);

}


