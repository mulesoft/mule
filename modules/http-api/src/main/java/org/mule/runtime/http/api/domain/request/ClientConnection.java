/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.request;

import org.mule.runtime.http.api.server.HttpServer;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;

/**
 * Representation of all client related data concerning an {@link HttpServer}.
 *
 * @since 4.0
 */
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
