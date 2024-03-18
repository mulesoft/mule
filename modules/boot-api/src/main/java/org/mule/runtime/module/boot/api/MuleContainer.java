/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.api;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Represents a Mule Container instance. It can be started and gracefully shut down.
 *
 * @since 4.5
 */
public interface MuleContainer {

  /**
   * Starts the Mule Container.
   * <p>
   * This method must return after the deployment has started, leaving container running.
   *
   * @param configurationsReady     Allows for querying the readiness state of the bootstrapping configurations (which may be
   *                                still running asynchronously). Implementations should check this before showing the splash
   *                                screen or starting any kind of deployment.
   * @param additionalSplashEntries Custom entries to add to the splash screen. To be controlled by the bootstrapping application.
   * @throws Exception If there was any issue during the startup of the Container or finishing with any asynchronous bootstrapping
   *                   configuration. The state of the Container should be as if never started. The bootstrapping application must
   *                   exit after this.
   */
  void start(Future<Boolean> configurationsReady, List<String> additionalSplashEntries) throws Exception;

  /**
   * Shuts down the Container gracefully, stopping all the associated tasks and cleaning up resources.
   *
   * @throws Exception If there was any issue stopping the Container.
   */
  void shutdown() throws Exception;
}
