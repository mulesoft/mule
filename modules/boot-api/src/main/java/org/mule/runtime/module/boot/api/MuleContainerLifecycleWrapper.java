/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.api;

import org.mule.runtime.module.boot.internal.MuleContainerWrapperProvider;

/**
 * Manages the lifecycle of the {@link MuleContainer}.
 *
 * @since 4.6
 */
public interface MuleContainerLifecycleWrapper {

  /**
   * Creates the implementation instance.
   *
   * @return The {@link MuleContainerLifecycleWrapper} implementation.
   */
  public static MuleContainerLifecycleWrapper getMuleContainerWrapper() {
    return MuleContainerWrapperProvider.getMuleContainerWrapper();
  }

  /**
   * Requests that the JVM be restarted but then returns. This allows components to initiate a JVM exit and then continue,
   * allowing a normal shutdown initiated by the JVM via shutdown hooks.
   */
  void restart();

  /**
   * Requests that the JVM be shutdown but then returns. This allows components to initiate a JVM exit and then continue, allowing
   * a normal shutdown initiated by the JVM via shutdown hooks.
   *
   * @param exitCode The exit code that the bootstrapping application should return to the OS.
   */
  void stop(int exitCode);

}
