/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.sdk.api.runtime.exception.ExceptionHandler;
import org.mule.sdk.api.runtime.exception.ExceptionHandlerFactory;

public final class DefaultExceptionHandlerFactory implements ExceptionHandlerFactory {

  private final SdkExceptionHandlerAdapter enricher;

  public DefaultExceptionHandlerFactory(Class<?> enricherType) {
    checkArgument(enricherType != null, "ExceptionEnricher type cannot be null");
    Object instance;
    try {
      instance = ClassUtils.instantiateClass(enricherType);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create ExceptionEnricher of type " + enricherType.getName()),
                                     e);
    }
    if (instance instanceof org.mule.runtime.extension.api.runtime.exception.ExceptionHandler) {
      enricher = new SdkExceptionHandlerAdapter((org.mule.runtime.extension.api.runtime.exception.ExceptionHandler) instance);
    } else if (instance instanceof ExceptionHandler) {
      enricher = new SdkExceptionHandlerAdapter((ExceptionHandler) instance);
    } else {
      throw new IllegalModelDefinitionException(format("Exception handler of class '%s' must extend either %s or %s.",
                                                       enricherType.getName(),
                                                       org.mule.runtime.extension.api.runtime.exception.ExceptionHandler.class
                                                           .getName(),
                                                       ExceptionHandler.class.getName()));
    }
  }

  @Override
  public ExceptionHandler createHandler() {
    return enricher;
  }
}
