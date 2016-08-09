/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import java.util.concurrent.ThreadFactory;

/**
 * A {@link ThreadFactory} which creates a thread of name {@link #THREAD_NAME} and which TCCL is {@link #contextClassLoader}
 */
final class LoggerContextReaperThreadFactory implements ThreadFactory {

  static final String THREAD_NAME = "logger.context.reaper";
  private final ClassLoader contextClassLoader;

  /**
   * Creates a new instance for the given {@code contextClassLoader}
   *
   * @param contextClassLoader the {@link ClassLoader} which should be used as TCCL of the created threads
   */
  LoggerContextReaperThreadFactory(ClassLoader contextClassLoader) {
    this.contextClassLoader = contextClassLoader;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(runnable, THREAD_NAME);
    thread.setContextClassLoader(contextClassLoader);

    return thread;
  }
}
