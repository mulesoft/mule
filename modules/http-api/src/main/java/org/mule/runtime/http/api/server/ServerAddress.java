/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
