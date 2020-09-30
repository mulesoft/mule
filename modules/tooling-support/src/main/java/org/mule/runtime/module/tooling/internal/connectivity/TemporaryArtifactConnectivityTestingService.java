/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.connectivity;

import static com.google.common.base.Throwables.getCausalChain;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticService;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;


/**
 * {@link ConnectivityTestingService} for a temporary artifact.
 * <p/>
 * This class will take care of the artifact initialization as part of the connection service invocation and deal with any thrown
 * exception by the startup of the artifact.
 */
public class TemporaryArtifactConnectivityTestingService extends AbstractArtifactAgnosticService
    implements ConnectivityTestingService {

  /**
   * Creates a {@code DefaultConnectivityTestingService}.
   *
   * @param applicationSupplier supplier of the application that will be used to do connectivity testing.
   */
  TemporaryArtifactConnectivityTestingService(ApplicationSupplier applicationSupplier) {
    super(applicationSupplier);
  }

  /**
   * {@inheritDoc}
   *
   * @throws MuleRuntimeException
   */
  @Override
  public ConnectionValidationResult testConnection(Location location) {
    checkArgument(location != null, "identifier cannot be null");
    return withTemporaryApplication(
                                    application -> application.getConnectivityTestingService().testConnection(location),
                                    e -> {
                                      if (e instanceof DeploymentStartException) {
                                        return failure(e.getMessage(), e);
                                      } else {
                                        return getCausalChain(e).stream()
                                            .filter(exception -> exception.getClass().equals(ConnectionException.class)
                                                && ((ConnectionException) exception).getErrorType().isPresent())
                                            .map(exception -> failure(exception.getMessage(),
                                                                      ((ConnectionException) exception).getErrorType().get(),
                                                                      (Exception) exception))
                                            .findFirst()
                                            .orElse(failure(e.getMessage(), e));
                                      }
                                    });
  }
}
