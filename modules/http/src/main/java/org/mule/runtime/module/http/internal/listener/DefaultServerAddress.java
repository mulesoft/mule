/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static org.mule.runtime.module.http.api.HttpConstants.ALL_INTERFACES_IP;
import org.mule.service.http.api.server.ServerAddress;

public class DefaultServerAddress implements ServerAddress {

  private final String ip;
  private int port;

  public DefaultServerAddress(String ip, int port) {
    this.port = port;
    this.ip = ip;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public String getIp() {
    return ip;
  }

  @Override
  public boolean overlaps(ServerAddress serverAddress) {
    return (port == serverAddress.getPort()) && (isAllInterfaces(this) || isAllInterfaces(serverAddress));
  }

  public static boolean isAllInterfaces(ServerAddress serverAddress) {
    return ALL_INTERFACES_IP.equals(serverAddress.getIp());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServerAddress that = (ServerAddress) o;

    if (port != that.getPort()) {
      return false;
    }
    if (!ip.equals(that.getIp())) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = ip.hashCode();
    result = 31 * result + port;
    return result;
  }

  @Override
  public String toString() {
    return "ServerAddress{" + "ip='" + ip + '\'' + ", port=" + port + '}';
  }
}
