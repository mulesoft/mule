/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property.wrappertype;

import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import java.util.Stack;

/**
 * {@link ModelProperty} implementation which communicates which are the true types of a certain parameter, due
 * that these ones are erased from the {@link ParameterModel}.
 * </p>
 * The model property through {@link WrapperTypesModelProperty#getWrapperTypes()} will communicate the {@link Stack} of
 * erased types for the parameter.
 *
 * @since 4.0
 */
public class WrapperTypesModelProperty implements ModelProperty {

  private Stack<WrapperType> wrapperTypes;

  WrapperTypesModelProperty(Stack<WrapperType> wrapperTypes) {
    checkState(!wrapperTypes.empty(), "Stack should not be empty");
    this.wrapperTypes = wrapperTypes;
  }

  /**
   * @return A new {@link WrapperTypesModelProperty} {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "wrapperTypes";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * This gives the {@link Stack} of {@link WrapperType}, which represents the erased types for a parameter.
   * For example, if a parameter was originally: {@code ParameterResolver<TypedValue<String>>}, this {@link Stack}
   * will contain the following content {@code TypedValue -> ParameterResolver}
   *
   * @return The {@link Stack} of {@link WrapperType wrapper types} for the enriched parameter
   */
  public Stack<WrapperType> getWrapperTypes() {
    Stack<WrapperType> objects = new Stack<>();
    objects.addAll(wrapperTypes);
    return objects;
  }

  public static class Builder {

    private Stack<WrapperType> wrapperTypes = new Stack<>();

    public Builder addWrapperType(WrapperType wrapperType) {
      wrapperTypes.add(wrapperType);
      return this;
    }

    public WrapperTypesModelProperty build() {

      return new WrapperTypesModelProperty(wrapperTypes);
    }
  }
}
