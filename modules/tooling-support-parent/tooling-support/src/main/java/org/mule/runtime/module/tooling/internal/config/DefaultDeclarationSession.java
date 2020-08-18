/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static java.lang.String.format;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.module.tooling.internal.utils.ActionFunction.actionCallWrapper;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.ValueResult;
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

  private static final String SERVICE_NAME = DefaultDeclarationSession.class.getSimpleName();
  private Logger LOGGER = LoggerFactory.getLogger(DefaultDeclarationSession.class);
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
    return internalDeclarationSession;
  }

  private <T> T withInternalDeclarationSession(String functionName, Function<DeclarationSession, T> function) {
    DeclarationSession declarationSession = getInternalDeclarationSession();
    return actionCallWrapper(() -> function.apply(declarationSession), SERVICE_NAME, functionName);
  }

  private DeclarationSession getInternalDeclarationSession() {
    return this.internalDeclarationSession.get();
  }

  @Override
  public ConnectionValidationResult testConnection(String configName) {
    try {
      return withInternalDeclarationSession("testConnection()", session -> session.testConnection(configName));
    } catch (Exception e) {
      LOGGER.error(format("Unknown error while performing test connection on config: '%s'", configName), e);
      return failure(format("Unknown error while performing test connection on config: '%s'. %s", configName,
                            getRootCauseMessage(e)),
                     e);
    }
  }

  @Override
  public ValueResult getValues(ParameterizedElementDeclaration component, String parameterName) {
    try {
      return withInternalDeclarationSession("getValues()", session -> session.getValues(component, parameterName));
    } catch (NoClassDefFoundError | Exception e) {
      LOGGER.error(format("Unknown error while resolving values on component: '%s' for parameter: '%s'", component.getName(),
                          parameterName),
                   e);
      return resultFrom(newFailure()
          .withMessage(format("Unknown error while resolving values for parameter: '%s'. %s", parameterName,
                              getRootCauseMessage(e)))
          .withReason(getStackTrace(e))
          .build());
    }
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("getMetadataKeys()", session -> session.getMetadataKeys(component));
    } catch (NoClassDefFoundError | Exception e) {
      LOGGER.error(format("Unknown error while resolving metadata keys on component: '%s'", component.getName()), e);
      return MetadataResult.failure(MetadataFailure.Builder.newFailure()
          .withMessage(format("Unknown error while resolving metadata keys on component: '%s'. %s", component.getName(),
                              getRootCauseMessage(e)))
          .withReason(getStackTrace(e))
          .withFailureCode(UNKNOWN)
          .onComponent());
    }
  }

  @Override
  public MetadataResult<ComponentMetadataTypesDescriptor> resolveComponentMetadata(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("resolveComponentMetadata()", session -> session.resolveComponentMetadata(component));
    } catch (NoClassDefFoundError | Exception e) {
      LOGGER.error(format("Unknown error while resolving metadata on component: '%s'", component.getName()), e);
      return MetadataResult.failure(MetadataFailure.Builder.newFailure()
          .withMessage(format("Unknown error while resolving metadata on component: '%s'. %s", component.getName(),
                              getRootCauseMessage(e)))
          .withReason(getStackTrace(e))
          .withFailureCode(UNKNOWN)

          .onComponent());
    }
  }

  @Override
  public void dispose() {
    super.dispose();
  }

}
