/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
