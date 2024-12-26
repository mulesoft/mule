/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.concurrent;

import static java.lang.String.format;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.core.api.util.StringUtils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class NamedThreadFactory implements java.util.concurrent.ThreadFactory {

  private final String name;
  private final AtomicLong counter;
  private final ClassLoader contextClassLoader;
  private final ThreadGroup threadGroup;

  public NamedThreadFactory(String name) {
    this(name, null, null);
  }

  public NamedThreadFactory(String name, ClassLoader contextClassLoader) {
    this(name, contextClassLoader, null);
  }

  public NamedThreadFactory(String name, ClassLoader contextClassLoader, ThreadGroup threadGroup) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("NamedThreadFactory must have a proper name.");
    }

    this.name = name;
    this.contextClassLoader = contextClassLoader;
    this.threadGroup = threadGroup;
    this.counter = new AtomicLong(1);
  }

  @Override
  public Thread newThread(Runnable runnable) {
    Supplier<Thread> tf = () -> {
      Thread t;
      if (threadGroup != null) {
        t = new Thread(threadGroup, runnable);
      } else {
        t = new Thread(runnable);
      }
      configureThread(t);
      return t;
    };

    if (contextClassLoader != null) {
      return withContextClassLoader(this.getClass().getClassLoader(), tf::get);
    } else {
      return tf.get();
    }
  }

  protected void configureThread(Thread t) {
    if (contextClassLoader != null) {
      t.setContextClassLoader(contextClassLoader);
    }
    doConfigureThread(t);
  }

  protected void doConfigureThread(Thread t) {
    t.setName(format("%s.%02d", name, counter.getAndIncrement()));
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
