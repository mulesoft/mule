/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.runtime.exception.SdkExceptionHandlerFactory;
import org.mule.sdk.api.runtime.exception.ExceptionHandler;

public final class DefaultExceptionHandlerFactory implements SdkExceptionHandlerFactory {

  private final ExceptionHandler enricher;

  public DefaultExceptionHandlerFactory(Class<?> enricherType) {
    checkArgument(enricherType != null, "ExceptionEnricher type cannot be null");
    try {
      enricher = SdkExceptionHandlerAdapter.from(ClassUtils.instantiateClass(enricherType));
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
