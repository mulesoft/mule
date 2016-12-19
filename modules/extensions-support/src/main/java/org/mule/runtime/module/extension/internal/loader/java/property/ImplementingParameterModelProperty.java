/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import java.lang.reflect.Parameter;

/**
 * An immutable model property which indicates that the owning {@link EnrichableModel} was derived from a given {@link #parameter}
 *
 * @since 4.0
 */
public final class ImplementingParameterModelProperty implements ModelProperty {

  private final Parameter parameter;

  /**
   * Creates a new instance referencing the given {@code parameter}
   *
   * @param parameter a {@link Parameter} which defines the owning {@link ParameterModel}
   * @throws IllegalArgumentException if {@code parameter} is {@code null}
   */
  public ImplementingParameterModelProperty(Parameter parameter) {
    checkArgument(parameter != null, "parameter cannot be null");
    this.parameter = parameter;
  }

  /**
   * @return a {@link Parameter} which defines the owning {@link ParameterModel}
   */
  public Parameter getParameter() {
    return parameter;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code implementingParameter}
   */
  @Override
  public String getName() {
    return "implementingParameter";
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
