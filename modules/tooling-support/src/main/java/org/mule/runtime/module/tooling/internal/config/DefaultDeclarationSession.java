/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.FailureCode.CONNECTION_FAILURE;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.repository.api.BundleNotFoundException;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticService;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;
import org.mule.runtime.oauth.api.exception.RequestAuthenticationException;
import org.mule.runtime.oauth.api.exception.TokenNotFoundException;
import org.mule.runtime.oauth.api.exception.TokenUrlResponseException;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDeclarationSession extends AbstractArtifactAgnosticService implements DeclarationSession {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDeclarationSession.class);
  private LazyValue<DeclarationSession> internalDeclarationSession;

  DefaultDeclarationSession(ApplicationSupplier applicationSupplier) {
    super(applicationSupplier);
    this.internalDeclarationSession = new LazyValue<>(() -> {
      try {
        return createInternalService(getStartedApplication());
      } catch (ApplicationStartingException e) {
        LOGGER.error("Unknown error while starting temporary application for declaration", e);
        throw new MuleRuntimeException(e.getCause());
      }
    });
  }

  private DeclarationSession createInternalService(Application application) {
    long startTime = currentTimeMillis();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Creating declaration session to delegate calls");
    }

    final InternalDeclarationSession internalDataProviderService =
        new InternalDeclarationSession(application.getDescriptor().getArtifactDeclaration());
    InternalDeclarationSession internalDeclarationSession = application.getRegistry()
        .lookupByType(MuleContext.class)
        .map(muleContext -> {
          try {
            return muleContext.getInjector().inject(internalDataProviderService);
          } catch (MuleException e) {
            throw new MuleRuntimeException(createStaticMessage("Could not inject values into DeclarationSession"));
          }
        })
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find injector to create InternalDeclarationSession")));
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Creation of declaration session to delegate calls took [{}ms]", currentTimeMillis() - startTime);
    }

    return internalDeclarationSession;
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
    } catch (BundleNotFoundException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.error(format("Unknown error while performing test connection on config: '%s'", configName), e);
      return failure(format("Unknown error while performing test connection on config: '%s'. %s", configName,
                            getRootCauseMessage(e)),
                     e);
    }
  }

  @Override
  public ValueResult getValues(ParameterizedElementDeclaration component, String providerName) {
    try {
      return withInternalDeclarationSession("getValues()", session -> session.getValues(component, providerName));
    } catch (BundleNotFoundException e) {
      throw e;
    } catch (NoClassDefFoundError | Exception e) {
      if (isOAuthRootCauseException(e)) {
        LOGGER.error(format("OAuth Service error while resolving values on component: '%s:%s' for providerName: '%s'",
                            component.getDeclaringExtension(),
                            component.getName(), providerName),
                     e);

        return resultFrom(newFailure()
            .withFailureCode(CONNECTION_FAILURE.getName())
            .withMessage(getRootCauseMessage(e))
            .withReason(getStackTrace(e))
            .build());
      }

      LOGGER.error(format("Unknown error while resolving values on component: '%s:%s' for providerName: '%s'",
                          component.getDeclaringExtension(),
                          component.getName(), providerName),
                   e);
      return resultFrom(newFailure(e)
          .withMessage(format("Unknown error while resolving values on component: '%s:%s' for providerName: '%s'",
                              component.getDeclaringExtension(), component.getName(),
                              providerName,
                              getRootCauseMessage(e)))
          .withReason(getStackTrace(e))
          .build());
    }
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("getMetadataKeys()", session -> session.getMetadataKeys(component));
    } catch (BundleNotFoundException e) {
      throw e;
    } catch (NoClassDefFoundError | Exception e) {
      if (isOAuthRootCauseException(e)) {
        LOGGER.error(format("OAuth Service error while resolving metadata keys on component: '%s:%s'",
                            component.getDeclaringExtension(),
                            component.getName()),
                     e);

        return MetadataResult.failure(MetadataFailure.Builder.newFailure()
            .withFailureCode(CONNECTION_FAILURE)
            .withMessage(getRootCauseMessage(e))
            .withReason(getStackTrace(e))
            .onKeys());
      }

      LOGGER.error(format("Unknown error while resolving metadata keys on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   e);
      return MetadataResult.failure(MetadataFailure.Builder.newFailure()
          .withMessage(format("Unknown error while resolving metadata keys on component: '%s:%s'. %s",
                              component.getDeclaringExtension(),
                              component.getName(), getRootCauseMessage(e)))
          .withReason(getStackTrace(e))
          .withFailureCode(UNKNOWN)
          .onComponent());
    }
  }

  @Override
  public MetadataResult<ComponentMetadataTypesDescriptor> resolveComponentMetadata(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("resolveComponentMetadata()", session -> session.resolveComponentMetadata(component));
    } catch (BundleNotFoundException e) {
      throw e;
    } catch (NoClassDefFoundError | Exception e) {
      if (isOAuthRootCauseException(e)) {
        LOGGER
            .error(format("OAuth Service error while resolving metadata on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   e);

        return MetadataResult.failure(MetadataFailure.Builder.newFailure()
            .withFailureCode(CONNECTION_FAILURE)
            .withMessage(getRootCauseMessage(e))
            .withReason(getStackTrace(e))
            .onComponent());
      }

      LOGGER.error(format("Unknown error while resolving metadata on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   e);
      return MetadataResult.failure(MetadataFailure.Builder.newFailure()
          .withMessage(format("Unknown error while resolving metadata on component: '%s:%s'. %s",
                              component.getDeclaringExtension(),
                              component.getName(), getRootCauseMessage(e)))
          .withReason(getStackTrace(e))
          .withFailureCode(UNKNOWN)
          .onComponent());
    }
  }

  @Override
  public SampleDataResult getSampleData(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("getSampleData()", session -> session.getSampleData(component));
    } catch (BundleNotFoundException e) {
      throw e;
    } catch (NoClassDefFoundError | Exception e) {
      if (isOAuthRootCauseException(e)) {
        LOGGER.error(format("OAuth Service error while retrieving sample data on component: '%s:%s'",
                            component.getDeclaringExtension(),
                            component.getName()),
                     e);

        return SampleDataResult.resultFrom(SampleDataFailure.Builder.newFailure(e)
            .withFailureCode(CONNECTION_FAILURE.getName())
            .withMessage(getRootCauseMessage(e))
            .withReason(getStackTrace(e))
            .build());
      }

      LOGGER.error(format("Unknown error while retrieving sample data on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   e);
      return SampleDataResult.resultFrom(SampleDataFailure.Builder.newFailure(e)
          .withMessage(format("Unknown error while resolving sample data on component: '%s:%s'. %s",
                              component.getDeclaringExtension(),
                              component.getName(), getRootCauseMessage(e)))
          .withReason(getStackTrace(e))
          .build());
    }
  }

  private boolean isOAuthRootCauseException(Throwable e) {
    Throwable rootCause = getRootCause(e);
    return rootCause instanceof TokenUrlResponseException ||
        rootCause instanceof TokenNotFoundException ||
        rootCause instanceof RequestAuthenticationException;
  }

  @Override
  public void dispose() {
    super.dispose();
  }

}
