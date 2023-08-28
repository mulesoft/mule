/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.domain.request;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.server.HttpServer;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;

/**
 * Representation of all client related data concerning an {@link HttpServer}.
 *
 * @since 4.0
 */
@NoImplement
public interface ClientConnection {

  /**
   * @return the host address from the client
   */
  InetSocketAddress getRemoteHostAddress();

  /**
   * @return the client certificate provided during the TLS client authentication, returns null if the TLS connection didn't
   *         require client authentication or if the connection is not using TLS.
   */
  Certificate getClientCertificate();

}
