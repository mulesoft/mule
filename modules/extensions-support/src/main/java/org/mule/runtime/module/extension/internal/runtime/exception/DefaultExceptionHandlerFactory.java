/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandlerFactory;

public final class DefaultExceptionHandlerFactory implements ExceptionHandlerFactory {

  private final ExceptionHandler enricher;

  public DefaultExceptionHandlerFactory(Class<? extends ExceptionHandler> enricherType) {
    checkArgument(enricherType != null, "ExceptionEnricher type cannot be null");
    try {
      enricher = enricherType.newInstance();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create ExceptionEnricher of type " + enricherType.getName()),
                                     e);
    }
  }

  @Override
  public ExceptionHandler createHandler() {
    return enricher;
  }
}
