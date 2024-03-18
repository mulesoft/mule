/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.joining;

import org.mule.runtime.module.boot.api.MuleContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

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
        if (!bootstrapConfigurer.configure()) {
          // This call is important to communicate the exit code to the native code
          haltAndCatchFire(0, null);
          return false;
        }
      }
      return true;
    } catch (BootstrapConfigurationException e) {
      haltAndCatchFire(e.getExitCode(), e.getMessage());
      return false;
    }
  }

  /**
   * @return A {@link Future} that will be completed when all configurations are ready, indicating whether the bootstrapping
   *         process shall progress forward.
   */
  protected Future<Boolean> getAllConfigurersReady() {
    return supplyAsync(this::waitAllConfigurersReady);
  }

  /**
   * Waits until all the configurers have finished their tasks.
   * <p>
   * Will {@link #haltAndCatchFire(int, String)} if there is any error.
   *
   * @return Whether the bootstrapping process shall progress forward.
   */
  private boolean waitAllConfigurersReady() {
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
    List<ConfigurerDisposalException> disposalExceptions = new ArrayList<>();
    for (BootstrapConfigurer bootstrapConfigurer : bootstrapConfigurers) {
      try {
        bootstrapConfigurer.dispose();
      } catch (Throwable t) {
        // Records the exceptions but don't throw just yet, to let the other Configurers to run their disposal.
        disposalExceptions.add(new ConfigurerDisposalException(bootstrapConfigurer, t));
      }
    }

    if (!disposalExceptions.isEmpty()) {
      if (disposalExceptions.size() == 1) {
        // If there was only 1 exception, just put it as the cause.
        throw new RuntimeException(format("Error disposing configurer: %s", disposalExceptions.get(0)),
                                   disposalExceptions.get(0));
      } else {
        // If there were multiple, put all of them as suppressed, so they are at the same level.
        String joinedMsgs = disposalExceptions.stream()
            .map(Throwable::toString)
            .collect(joining(",\n\t"));
        RuntimeException disposalException =
            new RuntimeException(format("Found multiple errors disposing the configurers: [\n\t%s\n]", joinedMsgs));
        disposalExceptions.forEach(disposalException::addSuppressed);
        throw disposalException;
      }
    }
  }

  /**
   * Creates the {@link MuleContainer} and starts it using the given arguments.
   *
   * @param muleContainerFactory Factory for creating the {@link MuleContainer} and its {@link ClassLoader}.
   * @param args                 The arguments to pass to the {@link MuleContainer}.
   */
  protected abstract void start(MuleContainerFactory muleContainerFactory, String[] args);

  private static class ConfigurerDisposalException extends Exception {

    private final String configurerClassName;

    public ConfigurerDisposalException(BootstrapConfigurer configurer, Throwable cause) {
      super(cause);
      this.configurerClassName = configurer.getClass().getName();
    }

    @Override
    public String toString() {
      return format("%s: %s", configurerClassName, super.toString());
    }
  }
}
