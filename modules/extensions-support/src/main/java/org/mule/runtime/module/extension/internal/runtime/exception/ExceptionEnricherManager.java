/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.core.util.ExceptionUtils.extractCauseOfType;
import static org.mule.runtime.core.util.ExceptionUtils.extractConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnrichableModel;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;

/**
 * Given a {@link RuntimeExtensionModel} and another {@link ExceptionEnrichableModel} such as {@link RuntimeSourceModel} or
 * {@link RuntimeOperationModel}, this class will inspect for the correct {@link ExceptionEnricher} if there is one.
 * <p>
 * It contains all the logic for operations and sources {@link Throwable} process and handling.
 *
 * @since 4.0
 */
public final class ExceptionEnricherManager {

  private final ExceptionEnricher exceptionEnricher;

  public ExceptionEnricherManager(RuntimeExtensionModel extensionModel, ExceptionEnrichableModel childEnrichableModel) {
    exceptionEnricher = findExceptionEnricher(extensionModel, childEnrichableModel);
  }

  public Exception processException(Throwable t) {
    Exception handledException = handleException(t);
    Exception exception = exceptionEnricher.enrichException(handledException);
    return exception != null ? exception : handledException;
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

  private ExceptionEnricher findExceptionEnricher(RuntimeExtensionModel extension, ExceptionEnrichableModel child) {
    Optional<ExceptionEnricherFactory> exceptionEnricherFactory = child.getExceptionEnricherFactory();
    if (!exceptionEnricherFactory.isPresent()) {
      exceptionEnricherFactory = extension.getExceptionEnricherFactory();
    }
    return exceptionEnricherFactory.isPresent() ? exceptionEnricherFactory.get().createEnricher() : new NullExceptionEnricher();
  }

  ExceptionEnricher getExceptionEnricher() {
    return exceptionEnricher;
  }
}
