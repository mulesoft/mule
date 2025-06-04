/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.api;

import static java.util.Collections.synchronizedMap;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.async.DefaultAsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.EventRoute;

/**
 * Specialization of {@link DefaultAsyncQueueFullPolicy} that calls registered callbacks to trigger alerts to help in
 * troubleshooting.
 *
 * @since 4.10
 */
public class MuleAlertingAsyncQueueFullPolicy extends DefaultAsyncQueueFullPolicy {

  // use the classloader as key so entries are GCd on app undeployment
  private static final Map<ClassLoader, Consumer<ClassLoader>> callbacks = synchronizedMap(new WeakHashMap<>());

  @Override
  public EventRoute getRoute(long backgroundThreadId, Level level) {
    synchronized (callbacks) {
      callbacks.entrySet().forEach(e -> e.getValue().accept(e.getKey()));
    }

    return super.getRoute(backgroundThreadId, level);
  }

  /**
   * Registers a callback to be called when {@link #getRoute(long, Level)} is called, before delegating to the superinterface.
   *
   * @param regionClassLoader the region classloader for the artifact that owns the callback the callback
   * @param callback          the callback to invoke
   */
  public static void register(ClassLoader regionClassLoader, Consumer<ClassLoader> callback) {
    callbacks.put(regionClassLoader, callback);
  }

}
