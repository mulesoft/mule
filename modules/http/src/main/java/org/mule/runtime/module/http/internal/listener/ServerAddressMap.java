/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.service.http.api.server.ServerAddress;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a wrapper for a map whose keys are {@link ServerAddress}s. It makes sure
 * that if an entry is not found we instead search for an entry with that same port but host 0.0.0.0.
 */
public class ServerAddressMap<T> {

  private Map<ServerAddress, T> internalMap;

  public ServerAddressMap() {
    this(new HashMap<>());
  }

  public ServerAddressMap(Map<ServerAddress, T> internalMap) {
    this.internalMap = internalMap;
  }

  public void put(ServerAddress serverAddress, T value) {
    internalMap.put(serverAddress, value);
  }

  public T get(Object key) {
    T value = internalMap.get(key);
    if (value == null) {
      // if there's no entry for the specific address, we need to check if there's one for all interfaces address.
      value = internalMap.get(new DefaultServerAddress(HttpConstants.ALL_INTERFACES_IP, ((ServerAddress) key).getPort()));
    }
    return value;
  }

  public boolean containsKey(Object key) {
    return internalMap.containsKey(key);
  }

  public T remove(Object key) {
    return internalMap.remove(key);
  }
}
