/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.concurrent;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;
import static java.security.AccessController.getContext;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;

import org.mule.runtime.core.api.util.StringUtils;

import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class NamedThreadFactory implements java.util.concurrent.ThreadFactory {

  private static final AccessControlContext ACCESS_CONTROL_CTX = getContext();

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
      Thread currentThread = currentThread();
      ClassLoader originalClassLoader = currentThread.getContextClassLoader();
      ClassLoader contextClassLoader = this.getClass().getClassLoader();
      setContextClassLoader(currentThread, originalClassLoader, contextClassLoader);
      try {
        // Avoid the created thread to inherit the security context of the caller thread's stack.
        // If the thread creation is triggered by a deployable artifact classloader, a reference to it would be kept by the
        // created thread without this doProvileged call.
        return doPrivileged((PrivilegedAction<Thread>) () -> tf.get(), ACCESS_CONTROL_CTX);
      } finally {
        setContextClassLoader(currentThread, contextClassLoader, originalClassLoader);
      }
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
