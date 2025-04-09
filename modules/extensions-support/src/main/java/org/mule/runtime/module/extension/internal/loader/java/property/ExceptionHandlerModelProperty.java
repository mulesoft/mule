/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.runtime.extension.api.runtime.exception.SdkExceptionHandlerFactory;

/**
 * Model property to be used on components which support the use of an {@link ExceptionHandler}. This property gives access to a
 * {@link SdkExceptionHandlerFactory} to be used to obtain such enricher.
 *
 * @since 4.0
 */
public final class ExceptionHandlerModelProperty implements ModelProperty {

  private final SdkExceptionHandlerFactory exceptionHandlerFactory;

  /**
   * Creates a new instance
   *
   * @param exceptionHandlerFactory a non null {@link SdkExceptionHandlerFactory}
   */
  public ExceptionHandlerModelProperty(SdkExceptionHandlerFactory exceptionHandlerFactory) {
    checkArgument(exceptionHandlerFactory != null, "exceptionHandlerFactory cannot be null");
    this.exceptionHandlerFactory = exceptionHandlerFactory;
  }

  /**
   * @return a {@link SdkExceptionHandlerFactory}
   */
  public SdkExceptionHandlerFactory getExceptionHandlerFactory() {
    return exceptionHandlerFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "exceptionHandler";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
