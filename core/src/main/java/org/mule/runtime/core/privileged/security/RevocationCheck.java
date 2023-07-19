/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.security;

import org.mule.api.annotation.NoImplement;

import java.security.KeyStore;
import java.security.cert.TrustAnchor;
import java.util.Set;

import javax.net.ssl.ManagerFactoryParameters;

/**
 * Interface for certificate revocation checkers that prepare a trust manager factory configuration.
 *
 * @since 4.1
 */
@NoImplement
public interface RevocationCheck {

  /**
   * Configures trust store factory parameters for certificate revocation checking according to the method implementation.
   *
   * @param trustStore          the trust store configured for the corresponding TLS context
   * @param defaultTrustAnchors the default trusted CAs from the JVM
   * @return the configured trust manager factory parameters
   */
  ManagerFactoryParameters configFor(KeyStore trustStore, Set<TrustAnchor> defaultTrustAnchors);
}
