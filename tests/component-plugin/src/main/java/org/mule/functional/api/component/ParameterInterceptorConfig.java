/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParameterInterceptorConfig extends AbstractComponent {

  private Map<String, List<Map<String, TypedValue<?>>>> intercepted = new HashMap<>();

  public void intercept(String reference, Map<String, TypedValue<?>> parameters) {
    synchronized (intercepted) {
      intercepted.computeIfAbsent(reference, k -> new ArrayList<>()).add(parameters);
    }
  }

  public Map<String, List<Map<String, TypedValue<?>>>> dump() {
    synchronized (intercepted) {
      try {
        return new HashMap<>(intercepted);
      } finally {
        intercepted.clear();
      }
    }
  }
}
