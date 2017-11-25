/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.request;

import org.mule.runtime.http.api.server.HttpServer;

import java.net.InetSocketAddress;

/**
 * Representation of all server related data concerning an {@link HttpServer}.
 *
 * @since 4.1
 */
public interface ServerConnection {

  /**
   * @return the host address from the server
   */
  InetSocketAddress getLocalHostAddress();

}
