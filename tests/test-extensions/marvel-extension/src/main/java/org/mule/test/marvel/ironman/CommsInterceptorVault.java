/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.ironman;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks list of comms classified by an issuer name
 *
 * @since 1.7.0
 */
@TypeDsl(allowInlineDefinition = false)
public class CommsInterceptorVault {

  private final Map<String, List<Object>> intercepted = new HashMap<>();

  public void intercept(String issuer, Object comm) {
    synchronized (intercepted) {
      intercepted.computeIfAbsent(issuer, k -> new ArrayList<>()).add(comm);
    }
  }

  public Map<String, Object> display() {
    synchronized (intercepted) {
      try {
        return new HashMap<>(intercepted);
      } finally {
        intercepted.clear();
      }
    }
  }
}
