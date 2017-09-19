/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.security;

import org.mule.runtime.core.internal.secutiry.tls.TlsPropertiesMapper;
import org.mule.runtime.core.internal.secutiry.tls.TlsPropertiesSocketFactory;
import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;

import java.io.IOException;

/**
 * Configure indirect key stores. TLS/SSL connections are made on behalf of an entity, which can be anonymous or identified by a
 * certificate - this interface specifies how a keystore can be used to provide the certificates (and associated private keys)
 * necessary for identification.
 * 
 * <p>
 * The information specified in this interface is used to configure a key store indirectly. For more information see the
 * documentation for the connector or protocol in question. The comments in
 * {@link TlsConfiguration} may also be useful.
 * </p>
 * 
 * <p>
 * <em>Programmers:</em> this information, once stored in and retrieved from properties via
 * {@link TlsPropertiesMapper}, will provide a key manager factory via the
 * {@link TlsDirectKeyStore} interface implemented by {@link TlsConfiguration}. This can be
 * associated with a socket factory via {@link TlsPropertiesSocketFactory}.
 * </p>
 */
public interface TlsIndirectKeyStore {

  /**
   * @return The location (resolved relative to the current classpath and file system, if possible) of the keystore that contains
   *         public certificates and private keys for identification.
   */
  String getClientKeyStore();

  /**
   * @param name The location of the keystore that contains public certificates and private keys for identification.
   * @throws IOException If the location cannot be resolved via the file system or classpath
   */
  void setClientKeyStore(String name) throws IOException;

  /**
   * @return The password used to protect the keystore itself
   */
  String getClientKeyStorePassword();

  /**
   * @param clientKeyStorePassword The password used to protect the keystore itself
   */
  void setClientKeyStorePassword(String clientKeyStorePassword);

  void setClientKeyStoreType(String clientKeyStoreType);

  /**
   * @return The type of keystore used in {@link #setClientKeyStore(String)}
   */
  String getClientKeyStoreType();

}


