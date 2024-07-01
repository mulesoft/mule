/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class HazelcastManager {

  private static HazelcastManager instance;
  private HazelcastInstance hazelcastInstance;
  private IMap<String, Boolean> tokenMap;

  private HazelcastManager() {
    initHazelcast();
  }

  public static HazelcastManager getInstance() {
    if (instance == null) {
      synchronized (HazelcastManager.class) {
        if (instance == null) {
          instance = new HazelcastManager();
        }
      }
    }
    return instance;
  }

  private void initHazelcast() {
    Config config = new Config();
    hazelcastInstance = Hazelcast.newHazelcastInstance(config);
    tokenMap = hazelcastInstance.getMap("tokenMap");
  }

  public IMap<String, Boolean> getTokenMap() {
    return tokenMap;
  }

  public void shutdown() {
    if (hazelcastInstance != null) {
      hazelcastInstance.shutdown();
    }
  }
}
