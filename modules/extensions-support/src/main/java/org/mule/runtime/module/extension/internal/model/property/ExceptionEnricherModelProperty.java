/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;

/**
 * Model property to be used on components which support the use of an {@link ExceptionEnricher}.
 * This property gives access to a {@link ExceptionEnricherFactory} to be used to obtain
 * such enricher.
 *
 * @since 4.0
 */
public final class ExceptionEnricherModelProperty implements ModelProperty {

  private final ExceptionEnricherFactory exceptionEnricherFactory;

  /**
   * Creates a new instance
   * @param exceptionEnricherFactory a non null {@link ExceptionEnricherFactory}
   */
  public ExceptionEnricherModelProperty(ExceptionEnricherFactory exceptionEnricherFactory) {
    Preconditions.checkArgument(exceptionEnricherFactory != null, "exceptionEnricherFactory cannot be null");
    this.exceptionEnricherFactory = exceptionEnricherFactory;
  }

  /**
   * @return a {@link ExceptionEnricherFactory}
   */
  public ExceptionEnricherFactory getExceptionEnricherFactory() {
    return exceptionEnricherFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "exceptionEnricher";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isExternalizable() {
    return false;
  }
}
