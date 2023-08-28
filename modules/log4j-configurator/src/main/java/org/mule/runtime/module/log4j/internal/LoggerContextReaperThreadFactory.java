/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.log4j.internal;

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
