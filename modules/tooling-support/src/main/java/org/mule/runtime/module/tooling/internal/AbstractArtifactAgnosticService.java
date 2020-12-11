/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static com.google.common.base.Throwables.getCausalChain;
import static com.google.common.base.Throwables.propagateIfPossible;
import static java.lang.System.currentTimeMillis;
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.repository.api.BundleNotFoundException;

import java.util.Optional;
import java.util.function.Function;

import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractArtifactAgnosticService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractArtifactAgnosticService.class);
  private final ApplicationSupplier applicationSupplier;

  private Application application;

  protected AbstractArtifactAgnosticService(ApplicationSupplier applicationSupplier) {
    this.applicationSupplier = applicationSupplier;
  }

  protected Application getStartedApplication() throws ApplicationStartingException {
    if (application == null) {
      long startTime = currentTimeMillis();
      try {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Creating application");
        }
        application = applicationSupplier.get();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Application: '{}' has been created in [{}ms]", application.getArtifactId(),
                       currentTimeMillis() - startTime);
        }
      } catch (Exception e) {
        Optional<RuntimeException> bundleNotFoundException = getCausalChain(e).stream()
            .filter(exception -> exception.getClass().equals(ArtifactNotFoundException.class)
                || exception.getClass().equals(ArtifactResolutionException.class))
            .findFirst().map(exception -> new BundleNotFoundException(exception));
        if (bundleNotFoundException.isPresent()) {
          throw bundleNotFoundException.get();
        }
        propagateIfPossible(e, MuleRuntimeException.class);
        throw new MuleRuntimeException(e);
      }
      try {
        startTime = currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Starting application: '{}'", application.getArtifactId());
        }
        application.install();
        application.init();
        application.start();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Application: '{}' has been started in [{}ms]", application.getArtifactId(),
                       currentTimeMillis() - startTime);
        }
      } catch (Exception e) {
        // Clean everything if there is an error
        dispose();
        throw new ApplicationStartingException(e);
      }
    }
    return application;
  }

  protected <T> T withTemporaryApplication(Function<Application, T> function,
                                           Function<Exception, T> errorHandler) {
    Application application;
    try {
      application = getStartedApplication();
      return function.apply(application);
    } catch (ApplicationStartingException e) {
      return errorHandler.apply(e.getCauseException());
    } finally {
      dispose();
    }
  }

  protected void dispose() {
    if (application != null) {
      disposeApp();
    }
  }

  private void disposeApp() {
    final Application finalApplication = application;
    doWithoutFail(finalApplication::stop);
    doWithoutFail(finalApplication::dispose);
    doWithoutFail(() -> deleteTree(finalApplication.getLocation()));
    application = null;
  }

  private void doWithoutFail(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      LOGGER.warn(e.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(), e);
      }
    }
  }

  //Just to separate runtime vs configuration exceptions
  protected static class ApplicationStartingException extends Exception {

    private ApplicationStartingException(Exception cause) {
      super(cause);
    }

    public Exception getCauseException() {
      return (Exception) this.getCause();
    }
  }


}
