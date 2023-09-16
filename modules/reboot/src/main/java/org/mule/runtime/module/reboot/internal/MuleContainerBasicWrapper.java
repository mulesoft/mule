/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import static java.lang.String.format;
import static java.lang.System.exit;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link MuleContainerWrapper} that interacts with the {@link MuleContainer} directly. Other implementations
 * may use an intermediary (e.g.: Tanuki).
 *
 * @since 4.5
 */
public class MuleContainerBasicWrapper extends AbstractMuleContainerWrapper {

  private MuleContainer muleContainer;
  private boolean isStarted = false;

  @Override
  protected void start(MuleContainerFactory muleContainerFactory, String[] args) {
    try {
      muleContainer = muleContainerFactory.create(args);
      startWithContainerClassLoader();
    } catch (Exception e) {
      muleContainer = null;
      throw new RuntimeException(e);
    }
  }

  @Override
  public void haltAndCatchFire(int exitCode, String message) {
    throw new RuntimeException(message);
  }

  @Override
  public void restart() {
    // Mimics the message printed by the Tanuki Wrapper (we have tests expecting this string)
    System.out.println("JVM requested a restart.");

    // TODO: NO-OP for now until we define if we actually need to support it.
    // Starting a new JVM will not be really achievable without some kind of wrapper.
    // We could aim for gracefully shutting down the current Container and creating a new Container from a new ClassLoader, but
    // we may face CL leaks (actually a quick test confirmed that, but we didn't want to spend additional time investigating for
    // now).
    // Just restarting the same Container instance will not be enough because there are static finals that need to be reset.
  }

  @Override
  public void stop(int exitCode) {
    dispose();

    // Gracefully shutdowns the container
    doStop();
    muleContainer = null;

    // Quits the JVM now
    exit(exitCode);
  }

  private void doStop() {
    try {
      if (muleContainer != null && isStarted) {
        muleContainer.shutdown();
        isStarted = false;
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private void startWithContainerClassLoader() throws Exception {
    Optional<Long> javaPid = getPid();
    List<String> additionalSplashEntries = asList("Wrapper PID: Running Wrapperless",
                                                  format("Java PID: %s", javaPid.map(String::valueOf).orElse("N/A")));
    ClassLoader originalClassLoader = currentThread().getContextClassLoader();
    currentThread().setContextClassLoader(muleContainer.getClass().getClassLoader());
    try {
      muleContainer.start(getAllConfigurersReady(), additionalSplashEntries);
      isStarted = true;
    } finally {
      currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private Optional<Long> getPid() {
    try {
      // We need to use reflection because this API is not available in Java 8 or less.
      Class<?> processHandleCls = Class.forName("java.lang.ProcessHandle");
      Object currentProcessHandle = processHandleCls.getMethod("current").invoke(null);
      return of((long) processHandleCls.getMethod("pid").invoke(currentProcessHandle));
    } catch (ReflectiveOperationException e) {
      return empty();
    }
  }
}
