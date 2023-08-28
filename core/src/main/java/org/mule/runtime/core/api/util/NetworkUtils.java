/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkUtils {

  private static final Map<String, InetAddress> addressPerHost = new ConcurrentHashMap<>();
  private static InetAddress localHost;

  private NetworkUtils() {
    // utility class only
  }

  public static InetAddress getLocalHost() throws UnknownHostException {
    if (localHost == null) {
      localHost = InetAddress.getLocalHost();
    }
    return localHost;
  }

  /**
   * Resolves a local IP for a host name.
   *
   * This method should not be used to resolve external host ips since it has a cache that can grow indefinitely.
   *
   * For performance reasons returns the ip and not the {@link java.net.InetAddress} since the {@link java.net.InetAddress}
   * performs logic each time it has to resolve the host address.
   *
   * @param host the host name
   * @return the host ip
   * @throws UnknownHostException
   */
  public static String getLocalHostIp(String host) throws UnknownHostException {
    return getLocalHostAddress(host).getHostAddress();
  }

  /**
   * Resolves a local IP for a host name.
   *
   * This method should not be used to resolve external host ips since it has a cache that can grow indefinitely.
   *
   * For performance reasons returns the ip and not the {@link java.net.InetAddress} since the {@link java.net.InetAddress}
   * performs logic each time it has to resolve the host address.
   *
   * @param host the host name
   * @return the host ip
   * @throws UnknownHostException
   */
  public static InetAddress getLocalHostAddress(String host) throws UnknownHostException {
    InetAddress ip = addressPerHost.get(host);
    if (ip == null) {
      ip = InetAddress.getByName(host);
      addressPerHost.put(host, ip);
    }
    return ip;
  }
}
