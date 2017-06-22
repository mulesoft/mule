/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import java.util.Collections;
import java.util.Map;

/**
 * A class holding cross-cutting startup info.
 */
public class StartupContext {

  private static final ThreadLocal<StartupContext> info = new ThreadLocal<StartupContext>() {

    @Override
    protected StartupContext initialValue() {
      return new StartupContext();
    }
  };

  private Map<String, Object> startupOptions = Collections.emptyMap();

  public static StartupContext get() {
    return info.get();
  }

  public Map<String, Object> getStartupOptions() {
    return Collections.unmodifiableMap(startupOptions);
  }

  public void setStartupOptions(Map<String, Object> startupOptions) {
    this.startupOptions = startupOptions;
  }
}
