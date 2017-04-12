/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import java.util.function.Function;

/**
 * Marker {@link ModelProperty} for {@link ParameterModel}s that indicates that the
 * enriched parameter is of {@link Function} type and resolve values of the generic type.
 * </p>
 * This model property is required, due that the {@link Function} wrapper type information
 * is missing once the {@link ExtensionModel} is built.
 *
 * @since 4.0
 */
public class FunctionParameterTypeModelProperty implements ModelProperty {

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "functionParameter";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
