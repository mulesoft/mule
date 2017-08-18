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
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.repository.api.BundleNotFoundException;

import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ConnectivityTestingService} for a temporary artifact.
 * <p/>
 * This class will take care of the artifact initialization as part of the connection service invocation and deal with any thrown
 * exception by the startup of the artifact.
 */
public class TemporaryArtifactConnectivityTestingService implements ConnectivityTestingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TemporaryArtifactConnectivityTestingService.class);
  private final ApplicationSupplier applicationSupplier;
  private Application application;

  /**
   * Creates a {@code DefaultConnectivityTestingService}.
   *
   * @param applicationSupplier supplier of the application that will be used to do connectivity testing.
   */
  public TemporaryArtifactConnectivityTestingService(ApplicationSupplier applicationSupplier) {
    this.applicationSupplier = applicationSupplier;
  }

  /**
   * {@inheritDoc}
   *
   * @throws MuleRuntimeException
   */
  @Override
  public ConnectionValidationResult testConnection(Location location) {
    checkArgument(location != null, "identifier cannot be null");
    try {
      application = applicationSupplier.get();
    } catch (Exception e) {
      throw getCausalChain(e).stream()
          .filter(exception -> exception.getClass().equals(ArtifactNotFoundException.class)
              || exception.getClass().equals(ArtifactResolutionException.class))
          .findFirst().map(exception -> (RuntimeException) new BundleNotFoundException(exception))
          .orElse(new MuleRuntimeException(e));
    }
    try {
      try {
        this.application.install();
        this.application.init();
        this.application.start();
      } catch (DeploymentStartException e) {
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
      return application.getConnectivityTestingService().testConnection(location);
    } finally {
      if (application != null) {
        doWithoutFail(() -> application.stop());
        doWithoutFail(() -> application.dispose());
        doWithoutFail(() -> deleteTree(application.getLocation()));
      }
    }
  }

  public void doWithoutFail(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      LOGGER.warn(e.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(), e);
      }
    }
  }

  @FunctionalInterface
  public interface ApplicationSupplier {

    Application get() throws Exception;

  }
}
