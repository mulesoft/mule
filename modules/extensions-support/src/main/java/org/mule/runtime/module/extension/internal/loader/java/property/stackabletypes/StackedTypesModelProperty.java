/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes;

import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.LazyValue;

import java.util.Optional;
import java.util.Set;
import java.util.Stack;

/**
 * {@link ModelProperty} implementation which communicates which are the true types of a certain parameter, due
 * that these ones are erased from the {@link ParameterModel}.
 * </p>
 * The model property through {@link StackedTypesModelProperty#getValueResolverFactory()} will communicate the {@link Stack} of
 * erased types for the parameter.
 *
 * @since 4.0
 */
public class StackedTypesModelProperty implements ModelProperty {

  private LazyValue<StackableTypesValueResolverFactory> factory;

  StackedTypesModelProperty(Stack<StackableType> stackableTypes) {
    checkState(!stackableTypes.empty(), "Stack can't be empty");
    this.factory = new LazyValue<>(() -> new StackableTypesValueResolverFactory(stackableTypes));
  }

  /**
   * @return A new {@link StackedTypesModelProperty} {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "stackableTypes";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * This gives the {@link Stack} of {@link StackableType}, which represents the erased types for a parameter.
   * For example, if a parameter was originally: {@code ParameterResolver<TypedValue<String>>}, this {@link Stack}
   * will contain the following content {@code TypedValue -> ParameterResolver}
   *
   * @return The {@link Stack} of {@link StackableType wrapper types} for the enriched parameter
   */
  public StackableTypesValueResolverFactory getValueResolverFactory() {
    return factory.get();
  }


  /**
   * Given a {@link Set} of {@link ModelProperty model properties} returns an {@link Optional} {@link StackedTypesModelProperty}
   *
   * @param modelProperties Model properties to introspect
   * @return an {@link Optional} {@link StackedTypesModelProperty}
   */
  public static Optional<StackedTypesModelProperty> getStackedTypesModelProperty(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().filter(mp -> mp instanceof StackedTypesModelProperty)
        .map(mp -> (StackedTypesModelProperty) mp).findFirst();
  }

  public static class Builder {

    private Stack<StackableType> stackableTypes = new Stack<>();

    public Builder addType(StackableType stackableType) {
      stackableTypes.add(stackableType);
      return this;
    }

    public StackedTypesModelProperty build() {

      return new StackedTypesModelProperty(stackableTypes);
    }
  }
}
