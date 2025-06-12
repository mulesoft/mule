/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;

import static java.lang.System.currentTimeMillis;

import static com.google.common.base.Throwables.getCausalChain;
import static com.google.common.base.Throwables.propagateIfPossible;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
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
        LOGGER.debug("Creating application");
        application = applicationSupplier.get();
        LOGGER.debug("Application: '{}' has been created in [{}ms]", application.getArtifactId(),
                     currentTimeMillis() - startTime);
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
        LOGGER.debug("Starting application: '{}'", application.getArtifactId());
        application.install();

        if (isLazyInit()) {
          try {
            application.lazyInitTooling(!isLazyInitEnableXmlValidations());
            // Force the initialization of the app components so that the registry is populated with the configs
            application.getArtifactContext().getRegistry().lookupByType(LazyComponentInitializer.class)
                .ifPresent(lazyInit -> lazyInit.initializeComponents(comp -> true));
          } catch (Exception e) {
            if (e.getCause() instanceof ConfigurationException) {
              // Keep thrown exception consistent with the previous implementation.
              throw new DeploymentInitException(createStaticMessage(e.getCause().getMessage()), e.getCause());
            } else {
              throw e;
            }
          }
        } else {
          application.initTooling();
        }

        application.start();
        LOGGER.debug("Application: '{}' has been started in [{}ms]", application.getArtifactId(),
                     currentTimeMillis() - startTime);
      } catch (Exception e) {
        // Clean everything if there is an error
        dispose();
        throw new ApplicationStartingException(e);
      }
    }
    return application;
  }

  private Boolean isLazyInit() {
    return application.getDescriptor().getDeploymentProperties()
        .map(deplProps -> (String) deplProps.getOrDefault(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, "false"))
        .map(Boolean::valueOf)
        .orElse(false);
  }

  private Boolean isLazyInitEnableXmlValidations() {
    return application.getDescriptor().getDeploymentProperties()
        .map(deplProps -> (String) deplProps.getOrDefault(MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY,
                                                          "false"))
        .map(Boolean::valueOf)
        .orElse(false);
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
      LOGGER.atDebug().setCause(e).log(e.getMessage());
    }
  }

  // Just to separate runtime vs configuration exceptions
  protected static class ApplicationStartingException extends Exception {

    private ApplicationStartingException(Exception cause) {
      super(cause);
    }

    public Exception getCauseException() {
      return (Exception) this.getCause();
    }
  }


}
