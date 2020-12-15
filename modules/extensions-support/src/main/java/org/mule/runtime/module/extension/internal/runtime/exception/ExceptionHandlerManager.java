/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;

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
   * It will check if there is a {@link SdkMethodInvocationException} wrapper exception
   * in the stacktrace wrapping the real failure and unwrap it.
   */
  public Throwable handleThrowable(Throwable e) {
    return e instanceof SdkMethodInvocationException ? e.getCause() : Exceptions.unwrap(e);
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
