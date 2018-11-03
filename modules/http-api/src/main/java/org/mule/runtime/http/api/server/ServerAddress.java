/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import org.mule.api.annotation.NoImplement;

import java.net.InetAddress;

/**
 * Representation of a server's address.
 *
 * @since 4.0
 */
@NoImplement
public interface ServerAddress {

  /**
   * @return the port this server is bound to.
   */
  int getPort();

  /**
   * @return the IP for this server as a String.
   */
  String getIp();

  /**
   * @return the address for this server.
   */
  InetAddress getAddress();

  /**
   * @param serverAddress another {@link ServerAddress}.
   * @return true if this and {@code serverAddress} have conflicting addresses, false otherwise.
   */
  boolean overlaps(ServerAddress serverAddress);
}
