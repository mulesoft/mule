/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import org.mule.runtime.module.boot.api.MuleContainer;
import org.mule.runtime.module.boot.api.MuleContainerLifecycleWrapper;

import org.apache.commons.cli.CommandLine;

/**
 * Manages the lifecycle of the {@link MuleContainer} including the execution of all the required {@link BootstrapConfigurer}s.
 * <p>
 * Implementations may interact with the underlying OS and handle signals/events.
 * <p>
 * Implementations must have a no-arguments constructor.
 *
 * @since 4.5
 */
public interface MuleContainerWrapper extends MuleContainerLifecycleWrapper {

  /**
   * Adds a {@link BootstrapConfigurer} to execute before starting.
   * <p>
   * The given {@link BootstrapConfigurer} will become managed by this wrapper instance, hence the disposal code will be executed
   * when this wrapper is disposed of. See {@link #dispose()}.
   *
   * @param bootstrapConfigurer The configurer to execute.
   */
  void addBootstrapConfigurer(BootstrapConfigurer bootstrapConfigurer);

  /**
   * Executes the registered {@link BootstrapConfigurer}, creates the {@link MuleContainer} using the given
   * {@code muleContainerFactory} and starts it.
   *
   * @param muleContainerFactory A {@link MuleContainerFactory} to use to create the {@link MuleContainer}.
   * @param commandLine          The command line parameters that were given to the bootstrapping application.
   */
  void configureAndStart(MuleContainerFactory muleContainerFactory, CommandLine commandLine);

  /**
   * Disposes any resources used by either the {@link BootstrapConfigurer}s or the {@link MuleContainerWrapper} implementation
   * itself.
   */
  void dispose();

  /**
   * Stops the bootstrapping sequence from progressing further. The bootstrapping application will eventually exit with the given
   * {@code exitCode} and the {@code message} should be available for troubleshooting.
   *
   * @param exitCode The exit code that the bootstrapping application should return to the OS.
   * @param message  A message to display for troubleshooting purposes.
   */
  void haltAndCatchFire(int exitCode, String message);

  /**
   * Requests that the JVM be restarted but then returns. This allows components to initiate a JVM exit and then continue,
   * allowing a normal shutdown initiated by the JVM via shutdown hooks.
   */
  @Override
  void restart();

  /**
   * Requests that the JVM be shutdown but then returns. This allows components to initiate a JVM exit and then continue, allowing
   * a normal shutdown initiated by the JVM via shutdown hooks.
   *
   * @param exitCode The exit code that the bootstrapping application should return to the OS.
   */
  @Override
  void stop(int exitCode);
}
