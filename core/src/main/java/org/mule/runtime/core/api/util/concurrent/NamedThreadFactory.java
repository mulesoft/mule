/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.concurrent;

import org.mule.runtime.core.api.util.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

public class NamedThreadFactory implements java.util.concurrent.ThreadFactory {

  private final String name;
  private final AtomicLong counter;
  private final ClassLoader contextClassLoader;

  public NamedThreadFactory(String name) {
    this(name, null);
  }

  public NamedThreadFactory(String name, ClassLoader contextClassLoader) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("NamedThreadFactory must have a proper name.");
    }

    this.name = name;
    this.contextClassLoader = contextClassLoader;
    this.counter = new AtomicLong(1);
  }

  public Thread newThread(Runnable runnable) {
    Thread t = new Thread(runnable);
    configureThread(t);
    return t;
  }

  protected void configureThread(Thread t) {
    if (contextClassLoader != null) {
      t.setContextClassLoader(contextClassLoader);
    }
    doConfigureThread(t);
  }

  protected void doConfigureThread(Thread t) {
    t.setName(String.format("%s.%02d", name, counter.getAndIncrement()));
  }

  public ClassLoader getContextClassLoader() {
    return contextClassLoader;
  }

  public AtomicLong getCounter() {
    return counter;
  }

  public String getName() {
    return name;
  }
}
