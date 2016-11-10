/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.artifact;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;

/**
 * Artifact meant to be used for tooling purposes.
 *
 * Artifacts must be construct without being started since the start may fail due to lifecycle issues to testing component in the
 * configuration. Some tooling services will required to do an start as part of some services to be aware of context creation
 * problems that may be, for instance, connectivity testing problems.
 *
 * @since 4.0
 */
public interface TemporaryArtifact extends Startable, Disposable {

  /**
   * Initialises and starts the tooling context.
   *
   * @throws MuleException which can be an {@link InitialisationException} or
   *         {@link org.mule.runtime.core.api.config.ConfigurationException} that represents a failure in the configuration of
   *         components.
   */
  void start() throws MuleException;

  /**
   * @return true if the artifact has already been created, false otherwise.
   */
  boolean isStarted();

  /**
   * @return connectivity testing service to test connection over configuration components
   */
  ConnectivityTestingService getConnectivityTestingService();

  /**
   * @return the {@code MuleContext} created by the artifact.
   */
  MuleContext getMuleContext();

  /**
   * Destroys all the resource allocated for the artifact.
   */
  void dispose();

}
