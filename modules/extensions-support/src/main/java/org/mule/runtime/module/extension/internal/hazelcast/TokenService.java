/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hazelcast;

import com.hazelcast.map.IMap;

public class TokenService {

  private final HazelcastManager hazelcastManager;

  public TokenService() {
    this.hazelcastManager = HazelcastManager.getInstance();
  }

  public void registerToken(String token) {
    IMap<String, Boolean> tokenMap = hazelcastManager.getTokenMap();
    tokenMap.put(token, false);
  }

  public void invalidateToken(String token) {
    IMap<String, Boolean> tokenMap = hazelcastManager.getTokenMap();
    tokenMap.put(token, true);
  }

  public boolean isTokenInvalidated(String token) {
    IMap<String, Boolean> tokenMap = hazelcastManager.getTokenMap();
    Boolean invalidated = tokenMap.get(token);
    return invalidated != null && invalidated;
  }
}
