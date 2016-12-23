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
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricher;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionEnricherModelProperty;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;

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

  public ExceptionEnricherManager(ExtensionModel extensionModel, ComponentModel componentModel) {
    exceptionEnricher = findExceptionEnricher(extensionModel, componentModel);
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

  private ExceptionEnricher findExceptionEnricher(ExtensionModel extension, EnrichableModel child) {
    return findExceptionEnricher(child).orElseGet(() -> findExceptionEnricher(extension).orElse(DEFAULT_EXCEPTION_ENRICHER));
  }

  private Optional<ExceptionEnricher> findExceptionEnricher(EnrichableModel model) {
    return model.getModelProperty(ExceptionEnricherModelProperty.class)
        .map(p -> p.getExceptionEnricherFactory().createEnricher());
  }

  ExceptionEnricher getExceptionEnricher() {
    return exceptionEnricher;
  }
}
