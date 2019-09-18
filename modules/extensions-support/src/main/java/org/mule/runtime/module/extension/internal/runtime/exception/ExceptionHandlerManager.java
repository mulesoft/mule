/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractCauseOfType;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;

/**
 * Given a {@link ExtensionModel} and another {@link EnrichableModel}, this class will
 * test for a {@link ExceptionHandlerModelProperty} to determine the {@link ExceptionHandler}
 * which should be use. If no such property is available then a default {@link NullExceptionHandler}
 * is used.
 * <p>
 * It also contains all the logic for operations and sources {@link Throwable} process and handling.
 *
 * @since 4.0
 */
public final class ExceptionHandlerManager {

  private static final ExceptionHandler DEFAULT_EXCEPTION_ENRICHER = new NullExceptionHandler();
  private final ExceptionHandler exceptionHandler;

  public ExceptionHandlerManager(ExtensionModel extensionModel, ComponentModel componentModel) {
    exceptionHandler = findExceptionHandler(extensionModel, componentModel);
  }

  /**
   * Process the {@link Throwable} parameter to obtain the correct failure and if its an exception this method will enrich it
   * with the obtained {@link ExceptionHandler} for this manager instance.
   */
  public Throwable process(Throwable t) {
    Throwable handled = handleThrowable(t);
    Throwable result = enrich(handled);
    return result != null ? result : handled;
  }

  /**
   * Given a {@link Throwable} instance this method will get the specific failure reason.
   * <p>
   * If there is a {@link ConnectionException} in the stacktrace is going to be considered the main failure reason,
   * otherwise it will check if there is a {@link UndeclaredThrowableException} reflective wrapper exception
   * in the stacktrace wrapping the real failure.
   */
  public Throwable handleThrowable(Throwable e) {
    Optional<ConnectionException> connectionException = extractConnectionException(e);
    if (connectionException.isPresent()) {
      return resolveConnectionException(connectionException.get(), e);
    } else {
      // unwraps the exception thrown by the reflective operation if exist any.
      return extractCauseOfType(e, UndeclaredThrowableException.class).orElse(e);
    }
  }

  private ConnectionException resolveConnectionException(ConnectionException connectionException, Throwable handledThrowable) {
    if (isMessagingExceptionWithConnectivityError(handledThrowable) && !connectionException.getErrorType().isPresent()) {
      return new ConnectionException(connectionException.getMessage(), connectionException.getCause(),
                                     ((MessagingException) handledThrowable).getEvent().getError().get().getErrorType());
    }
    return connectionException;
  }

  private boolean isMessagingExceptionWithConnectivityError(Throwable cause) {
    return cause instanceof MessagingException &&
        ((MessagingException) cause).getEvent().getError().isPresent() &&
        ((MessagingException) cause).getEvent().getError().get().getErrorType().getIdentifier()
            .equals(CONNECTIVITY_ERROR_IDENTIFIER);
  }

  private Throwable enrich(Throwable t) {
    return t instanceof Exception ? exceptionHandler.enrichException(((Exception) t)) : t;
  }

  private ExceptionHandler findExceptionHandler(ExtensionModel extension, EnrichableModel child) {
    return findExceptionHandler(child).orElseGet(() -> findExceptionHandler(extension).orElse(DEFAULT_EXCEPTION_ENRICHER));
  }

  private Optional<ExceptionHandler> findExceptionHandler(EnrichableModel model) {
    return model.getModelProperty(ExceptionHandlerModelProperty.class)
        .map(p -> p.getExceptionHandlerFactory().createHandler());
  }

  ExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }
}
