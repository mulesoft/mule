/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;

/**
 * Abstract implementation of {@link MuleContainerWrapper} which helps with the logic of calling the {@link BootstrapConfigurer}s
 * and handling the errors.
 *
 * @since 4.5
 */
public abstract class AbstractMuleContainerWrapper implements MuleContainerWrapper {

  private final Collection<BootstrapConfigurer> bootstrapConfigurers = new ArrayList<>();

  @Override
  public void addBootstrapConfigurer(BootstrapConfigurer bootstrapConfigurer) {
    bootstrapConfigurers.add(bootstrapConfigurer);
  }

  @Override
  public void configureAndStart(MuleContainerFactory muleContainerFactory, CommandLine commandLine) {
    if (configure()) {
      System.out.println("Starting the Mule Container...");
      // Only command line arguments not recognized by the bootstrapping application are passed down to the container
      start(muleContainerFactory, commandLine.getArgs());
    }
  }

  private boolean configure() {
    try {
      for (BootstrapConfigurer bootstrapConfigurer : bootstrapConfigurers) {
        bootstrapConfigurer.configure();
      }
      return true;
    } catch (BootstrapConfigurationException e) {
      haltAndCatchFire(e.getExitCode(), e.getMessage());
      return false;
    }
  }

  /**
   * Waits until all the configurers have finished their tasks.
   * <p>
   * Will {@link #haltAndCatchFire(int, String)} if there is any error.
   *
   * @return Whether the bootstrapping process can process forward.
   */
  protected boolean waitAllConfigurersReady() {
    try {
      for (BootstrapConfigurer bootstrapConfigurer : bootstrapConfigurers) {
        bootstrapConfigurer.await();
      }
      return true;
    } catch (BootstrapConfigurationException e) {
      haltAndCatchFire(e.getExitCode(), e.getMessage());
      return false;
    }
  }

  public void dispose() {
    for (BootstrapConfigurer bootstrapConfigurer : bootstrapConfigurers) {
      bootstrapConfigurer.dispose();
    }
  }

  /**
   * Creates the {@link MuleContainer} and starts it using the given arguments.
   *
   * @param muleContainerFactory Factory for creating the {@link MuleContainer} and its {@link ClassLoader}.
   * @param args The arguments to pass to the {@link MuleContainer}.
   */
  protected abstract void start(MuleContainerFactory muleContainerFactory, String[] args);
}
