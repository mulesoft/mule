/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

import static com.google.common.base.Throwables.propagateIfPossible;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticService;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDeclarationSession extends AbstractArtifactAgnosticService implements DeclarationSession {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDeclarationSession.class);
  private LazyValue<DeclarationSession> internalDeclarationSession;

  DefaultDeclarationSession(ApplicationSupplier applicationSupplier, ArtifactDeclaration artifactDeclaration) {
    super(applicationSupplier);
    this.internalDeclarationSession = new LazyValue<>(() -> {
      try {
        return createInternalService(getStartedApplication(), artifactDeclaration);
      } catch (ApplicationStartingException e) {
        Exception causeException = e.getCauseException();
        LOGGER.error("There was an error while starting temporary application for declaration session: {}",
                     getRootCauseMessage(causeException));
        propagateIfPossible(causeException, MuleRuntimeException.class);
        throw new MuleRuntimeException(causeException);
      }
    });
  }

  private DeclarationSession createInternalService(Application application, ArtifactDeclaration artifactDeclaration) {
    long startTime = currentTimeMillis();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Creating declaration session to delegate calls");
    }

    final InternalDeclarationSession internalDeclarationService =
        new InternalDeclarationSession(artifactDeclaration);
    final MuleContext muleContext = application.getArtifactContext().getMuleContext();
    if (muleContext == null) {
      throw new MuleRuntimeException(createStaticMessage("Could not find injector to create InternalDeclarationSession"));
    }

    try {
      InternalDeclarationSession internalDeclarationSession = muleContext.getInjector().inject(internalDeclarationService);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Creation of declaration session to delegate calls took [{}ms]", currentTimeMillis() - startTime);
      }

      return internalDeclarationSession;
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not inject values into DeclarationSession"));
    }

  }

  private <T> T withInternalDeclarationSession(String functionName, Function<DeclarationSession, T> function) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Calling function: '{}'", functionName);
    }
    DeclarationSession declarationSession = getInternalDeclarationSession();

    long initialTime = currentTimeMillis();
    try {
      return function.apply(declarationSession);
    } finally {
      long totalTimeSpent = currentTimeMillis() - initialTime;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Function: '{}' completed in [{}ms]", functionName, totalTimeSpent);
      }
    }
  }

  private DeclarationSession getInternalDeclarationSession() {
    return this.internalDeclarationSession.get();
  }

  @Override
  public ConnectionValidationResult testConnection(String configName) {
    try {
      return withInternalDeclarationSession("testConnection()", session -> session.testConnection(configName));
    } catch (Throwable t) {
      LOGGER.error(format("Error while performing test connection on config: '%s'", configName), t);
      throw t;
    }
  }

  @Override
  public ValueResult getValues(ParameterizedElementDeclaration component, String providerName) {
    try {
      return withInternalDeclarationSession("getValues()", session -> session.getValues(component, providerName));
    } catch (Throwable t) {
      LOGGER.error(format("Error while resolving values on component: '%s:%s' for providerName: '%s'",
                          component.getDeclaringExtension(),
                          component.getName(), providerName),
                   t);
      throw t;
    }
  }

  @Override
  public ValueResult getFieldValues(ParameterizedElementDeclaration component, String providerName, String targetSelector) {
    try {
      return withInternalDeclarationSession("getFieldValues()",
                                            session -> session.getFieldValues(component, providerName, targetSelector));
    } catch (Throwable t) {
      LOGGER
          .error(format("Error while resolving field values on component: '%s:%s' for providerName: '%s' with targetSelector: '%s'",
                        component.getDeclaringExtension(),
                        component.getName(), providerName,
                        targetSelector),
                 t);
      throw t;
    }
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("getMetadataKeys()", session -> session.getMetadataKeys(component));
    } catch (Throwable t) {
      LOGGER.error(format("Error while resolving metadata keys on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   t);
      throw t;
    }
  }

  @Override
  public MetadataResult<ComponentMetadataTypesDescriptor> resolveComponentMetadata(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("resolveComponentMetadata()", session -> session.resolveComponentMetadata(component));
    } catch (Throwable t) {
      LOGGER.error(format("Error while resolving metadata on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   t);
      throw t;
    }
  }

  @Override
  public void disposeMetadataCache(ComponentElementDeclaration component) {
    try {
      withInternalDeclarationSession("disposeMetadataCache()", session -> {
        session.disposeMetadataCache(component);
        return null;
      });
    } catch (Throwable t) {
      LOGGER.error(format("Error while disposing metadata on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   t);
      throw t;
    }
  }

  @Override
  public SampleDataResult getSampleData(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("getSampleData()", session -> session.getSampleData(component));
    } catch (Throwable t) {
      LOGGER.error(format("Error while retrieving sample data on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   t);
      throw t;
    }
  }

  @Override
  public void dispose() {
    super.dispose();
  }

}
