/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ExceptionUtils.extractCauseOfType;
import static org.mule.runtime.core.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getExtensionsErrorNamespace;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.exception.TypedException;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricher;
import org.mule.runtime.module.extension.internal.model.property.ExceptionEnricherModelProperty;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;
import java.util.Set;

/**
 * Given a {@link ExtensionModel} and another {@link EnrichableModel}, this class will
 * test for a {@link ExceptionEnricherModelProperty} to determine the {@link ExceptionEnricher}
 * which should be use. If no such property is available then a default {@link NullExceptionEnricher}
 * is used.
 * <p>
 * It also contains all the logic for operations and sources {@link Throwable} process and handling.
 *
 * @since 4.0
 */
public final class ExceptionEnricherManager {

  private static final ExceptionEnricher DEFAULT_EXCEPTION_ENRICHER = new NullExceptionEnricher();
  private final ExceptionEnricher exceptionEnricher;
  private final ErrorTypeRepository typeRepository;
  private final String extensionNamespace;
  private final Set<ErrorModel> allowedErrorTypes;
  private final ExtensionModel extensionModel;
  private final ComponentModel componentModel;

  public ExceptionEnricherManager(ExtensionModel extensionModel, ComponentModel componentModel,
                                  ErrorTypeRepository typeRepository) {
    this.typeRepository = typeRepository;
    this.extensionModel = extensionModel;
    this.componentModel = componentModel;

    exceptionEnricher = findExceptionEnricher(extensionModel, componentModel);
    allowedErrorTypes = componentModel.getErrorModels();
    extensionNamespace = getExtensionsErrorNamespace(extensionModel);
  }

  public Exception processException(Throwable t) {
    Exception handledException = handleException(t);
    Exception exception = exceptionEnricher.enrichException(handledException);
    if (exception instanceof ModuleException) {
      ErrorTypeDefinition errorDefinition = ((ModuleException) exception).getType();
      exception = handleTypedException(exception, errorDefinition);
    }
    return exception != null ? exception : handledException;
  }

  private Exception handleTypedException(Exception exception, ErrorTypeDefinition errorDefinition) {
    if (isAllowedError(errorDefinition)) {
      Optional<ErrorType> errorType = typeRepository.lookupErrorType(new ComponentIdentifier.Builder()
          .withNamespace(extensionNamespace)
          .withName(errorDefinition.getType())
          .build());

      if (errorType.isPresent()) {
        exception = new TypedException(exception.getCause(), errorType.get());
      } else {
        throw new MuleRuntimeException(createStaticMessage("The component '%s' from the connector '%s' attempted to throw '%s', but it was not registered "
            +
            "in the Error Repository", componentModel.getName(), extensionModel.getName(),
                                                           extensionNamespace + ":" + errorDefinition),
                                       exception.getCause());
      }
    } else {
      throw new MuleRuntimeException(createStaticMessage("The component '%s' from the connector '%s' attempted to throw '%s', but"
          +
          " only %s errors are allowed.", componentModel.getName(), extensionModel.getName(),
                                                         extensionNamespace + ":" + errorDefinition, allowedErrorTypes),
                                     exception.getCause());
    }
    return exception;
  }

  public Exception handleException(Throwable e) {
    Throwable handled;
    Optional<ConnectionException> connectionException = extractConnectionException(e);
    if (connectionException.isPresent()) {
      handled = connectionException.get();
    } else {
      // unwraps the exception thrown by the reflective operation if exist any.
      handled = extractCauseOfType(e, UndeclaredThrowableException.class).orElse(e);
    }
    return wrapInException(handled);
  }

  private Exception wrapInException(Throwable t) {
    return t instanceof Exception ? (Exception) t : new Exception(t);
  }

  private ExceptionEnricher findExceptionEnricher(ExtensionModel extension, EnrichableModel child) {
    return findExceptionEnricher(child).orElseGet(() -> findExceptionEnricher(extension).orElse(DEFAULT_EXCEPTION_ENRICHER));
  }

  private Optional<ExceptionEnricher> findExceptionEnricher(EnrichableModel model) {
    return model.getModelProperty(ExceptionEnricherModelProperty.class)
        .map(p -> p.getExceptionEnricherFactory().createEnricher());
  }

  private boolean isAllowedError(ErrorTypeDefinition errorTypeDefinition) {
    return allowedErrorTypes
        .stream()
        .anyMatch(errorModel -> errorModel.getType().equals(errorTypeDefinition.getType())
            && errorModel.getNamespace().equals(extensionNamespace));
  }

  ExceptionEnricher getExceptionEnricher() {
    return exceptionEnricher;
  }
}
