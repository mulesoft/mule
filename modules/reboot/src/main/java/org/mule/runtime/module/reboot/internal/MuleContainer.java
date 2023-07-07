/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Represents a Mule Container instance. It can be started and gracefully shut down.
 */
public interface MuleContainer {

  /**
   * Starts the Mule Container.
   * <p>
   * This method must return after the deployment has started, leaving container running.
   *
   * @param configurationsReadyBarrier A barrier that needs to be awaited before showing the splash screen or starting any kind of
   *                                   deployment.
   * @param additionalSplashEntries    Custom entries to add to the splash screen. To be controlled by the bootstrapping
   *                                   application.
   * @throws Exception If there was any issue during the startup of the Container or finishing with any asynchronous bootstrapping
   *                   configuration. The state of the Container should be as if never started. The bootstrapping application must
   *                   exit after this.
   */
  void start(BooleanSupplier configurationsReadyBarrier, List<String> additionalSplashEntries) throws Exception;

  /**
   * Shuts down the Container gracefully, stopping all the associated tasks and cleaning up resources.
   *
   * @throws Exception If there was any issue stopping the Container.
   */
  void shutdown() throws Exception;
}
