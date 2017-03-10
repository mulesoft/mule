/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static com.google.common.base.Throwables.getCausalChain;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifact;

/**
 * {@link ConnectivityTestingService} for a temporary artifact.
 * <p/>
 * This class will take care of the artifact initialization as part of the connection service invocation
 * and deal with any thrown exception by the startup of the artifact.
 */
public class TemporaryArtifactConnectivityTestingService implements ConnectivityTestingService {

  private TemporaryArtifact temporaryArtifact;

  /**
   * Creates a {@code DefaultConnectivityTestingService}.
   *
   * @param temporaryArtifact tooling artifact used to do connectivity testing
   */
  public TemporaryArtifactConnectivityTestingService(TemporaryArtifact temporaryArtifact) {
    this.temporaryArtifact = temporaryArtifact;
  }

  /**
   * {@inheritDoc}
   *
   * @throws MuleRuntimeException
   */
  @Override
  public ConnectionValidationResult testConnection(String identifier) {
    checkArgument(identifier != null, "identifier cannot be null");
    try {
      if (!temporaryArtifact.isStarted()) {
        try {
          temporaryArtifact.start();
        } catch (InitialisationException | ConfigurationException e) {
          return failure(e.getMessage(), e);
        } catch (Exception e) {
          return getCausalChain(e).stream()
              .filter(exception -> exception.getClass().equals(ConnectionException.class)
                  && ((ConnectionException) exception).getErrorType().isPresent())
              .map(exception -> failure(exception.getMessage(), ((ConnectionException) exception).getErrorType().get(),
                                        (Exception) exception))
              .findFirst()
              .orElse(failure(e.getMessage(), e));
        }
      }
      return temporaryArtifact.getConnectivityTestingService().testConnection(identifier);
    } finally {
      temporaryArtifact.dispose();
    }
  }
}
