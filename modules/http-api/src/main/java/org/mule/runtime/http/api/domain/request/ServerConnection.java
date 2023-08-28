/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.domain.request;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.server.HttpServer;

import java.net.InetSocketAddress;

/**
 * Representation of all server related data concerning an {@link HttpServer}.
 *
 * @since 4.1
 */
@NoImplement
public interface ServerConnection {

  /**
   * @return the host address from the server
   */
  InetSocketAddress getLocalHostAddress();

}
