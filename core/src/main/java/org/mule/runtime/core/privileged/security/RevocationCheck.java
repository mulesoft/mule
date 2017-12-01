/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.security;

import java.security.KeyStore;
import java.security.cert.TrustAnchor;
import java.util.Set;

import javax.net.ssl.ManagerFactoryParameters;

/**
 * Interface for certificate revocation checkers that prepare a trust manager factory configuration.
 *
 * @since 4.1
 */
public interface RevocationCheck {

  /**
   * Configures trust store factory parameters for certificate revocation checking according to
   * the method implementation.
   *
   * @param trustStore the trust store configured for the corresponding TLS context
   * @param defaultTrustAnchors the default trusted CAs from the JVM
   * @return the configured trust manager factory parameters
   */
  ManagerFactoryParameters configFor(KeyStore trustStore, Set<TrustAnchor> defaultTrustAnchors);
}
