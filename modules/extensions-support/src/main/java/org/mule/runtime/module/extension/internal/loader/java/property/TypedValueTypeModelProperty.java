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
import org.mule.runtime.api.metadata.TypedValue;

/**
 * Marker {@link ModelProperty} for components's {@link ParameterModel parameter models} that indicates that the
 * enriched parameter is of {@link TypedValue} type and resolve values of the generic type.
 * <p>
 * This model property is required, due that the {@link TypedValue} wrapper type information
 * is missing once the {@link ExtensionModel} is built.
 *
 * @see TypedValue
 * @since 4.0
 */
public class TypedValueTypeModelProperty implements ModelProperty {

  @Override
  public String getName() {
    return "typedValueParameter";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
