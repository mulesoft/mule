/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property.wrappertype;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Optional;
import java.util.Set;
import java.util.Stack;

/**
 * {@link ModelProperty} implementation which communicates which are the true types of a certain parameter, due
 * that these ones are erased from the {@link ParameterModel}.
 * </p>
 * The model property through {@link StackedTypesModelProperty#getStackedTypes()} will communicate the {@link Stack} of
 * erased types for the parameter.
 *
 * @since 4.0
 */
public class StackedTypesModelProperty implements ModelProperty {

  private Stack<StackedType> stackedTypes;

  StackedTypesModelProperty(Stack<StackedType> stackedTypes) {
    checkState(!stackedTypes.empty(), "Stack should not be empty");
    this.stackedTypes = stackedTypes;
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
    return "stackedTypes";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * This gives the {@link Stack} of {@link StackedType}, which represents the erased types for a parameter.
   * For example, if a parameter was originally: {@code ParameterResolver<TypedValue<String>>}, this {@link Stack}
   * will contain the following content {@code TypedValue -> ParameterResolver}
   *
   * @return The {@link Stack} of {@link StackedType wrapper types} for the enriched parameter
   */
  public Stack<StackedType> getStackedTypes() {
    Stack<StackedType> objects = new Stack<>();
    objects.addAll(stackedTypes);
    return objects;
  }

  public static class Builder {

    private Stack<StackedType> stackedTypes = new Stack<>();

    public Builder addWrapperType(StackedType stackedType) {
      stackedTypes.add(stackedType);
      return this;
    }

    public StackedTypesModelProperty build() {

      return new StackedTypesModelProperty(stackedTypes);
    }
  }

  /**
   * Given a expression value, the expected type and the stack of {@link StackedType wrapper types}, iterates it and
   * creates all the required {@link ValueResolver} and stacks them is necessary.
   *
   * @param expression                Expression value for the expression based value resolver.
   * @param expectedType              The expected type of the expression resolution
   * @param stackedTypesModelProperty Model property containing the stack of {@link StackedType wrapper types}
   * @return The expression based {@link ValueResolver}
   */
  public static ValueResolver getExpressionBasedValueResolver(String expression, Class expectedType,
                                                              StackedTypesModelProperty stackedTypesModelProperty) {
    Stack<StackedType> stackedTypes = stackedTypesModelProperty.getStackedTypes();
    StackedType stackedType = stackedTypes.pop();
    StackedType.ExpressionBasedResolverFactory resolverFactory = stackedType
        .getExpressionBasedResolverFactory()
        .orElseThrow(() -> new IllegalStateException(format("Unable to create an Expression Based ValueResolver of '%s' type. No ExpressionBasedResolverFactory was registered for this type.",
                                                            stackedType.getType().getSimpleName())));

    ValueResolver resolver = resolverFactory.getResolver(expression, expectedType);
    resolver = getWrapperValueResolver(resolver, stackedTypes);
    return resolver;
  }

  /**
   * Given a static value and the stack of {@link StackedType wrapper types}, iterates it and creates all the required
   * {@link ValueResolver} and stacks them is necessary.
   *
   * @param value                     The static value
   * @param stackedTypesModelProperty Model property containing the stack of {@link StackedType stacked types}
   * @return The static {@link ValueResolver}
   */
  public static ValueResolver getStaticValueResolver(Object value, StackedTypesModelProperty stackedTypesModelProperty) {
    Stack<StackedType> stackedTypes = stackedTypesModelProperty.getStackedTypes();
    StackedType stackedType = stackedTypes.pop();
    StackedType.StaticResolverFactory resolverFactory = stackedType
        .getStaticResolverFactory()
        .orElseThrow(() -> new IllegalStateException(format("Unable to create an Static ValueResolver of '%s' type. No StaticResolverFactory was registered for this type.",
                                                            stackedType.getType().getSimpleName())));

    ValueResolver resolver = resolverFactory.getResolver(value);
    resolver = getWrapperValueResolver(resolver, stackedTypes);
    return resolver;
  }

  /**
   * Given a static value, the stack of {@link StackedType stacked types} and the desired class for the static resolver, if
   * the class is not found as the root of the parameter type, an {@link Optional#empty()} will be returned.
   * Also, iterates it and creates all the required {@link ValueResolver} and stacks them is necessary.
   *
   * @param value                     The static value
   * @param stackedTypesModelProperty Model property containing the stack of {@link StackedType stacked types}
   * @return The static {@link ValueResolver}
   */
  public static Optional<ValueResolver> getStaticValueResolver(Object value, StackedTypesModelProperty stackedTypesModelProperty,
                                                               Class clazz) {
    Stack<StackedType> stackedTypes = stackedTypesModelProperty.getStackedTypes();
    StackedType stackedType = stackedTypes.get(stackedTypes.size() - 1);
    if (stackedType.getType().equals(clazz)) {
      return of(getStaticValueResolver(value, stackedTypesModelProperty));
    }
    return empty();
  }

  /**
   * Given a static value and the stack of {@link StackedType wrapper types}, iterates it and creates all the required
   * {@link ValueResolver} and stacks them is necessary.
   *
   * @param resolverDelegate The {@link ValueResolver} to wrap
   * @param stackedTypes     The stack of {@link StackedType wrapper types}
   * @return The wrapped {@link ValueResolver}
   */
  public static ValueResolver getWrapperValueResolver(ValueResolver resolverDelegate, Stack<StackedType> stackedTypes) {
    while (!stackedTypes.empty()) {
      StackedType delegateStackedType = stackedTypes.pop();
      StackedType.DelegateResolverFactory delegateResolverFactory = delegateStackedType
          .getDelegateResolverFactory()
          .orElseThrow(() -> new IllegalStateException(format("Unable to create a ValueResolver Wrapper of '%s' type. No DelegateResolverFactory was registered for this type.",
                                                              delegateStackedType.getType().getSimpleName())));
      resolverDelegate = delegateResolverFactory.getResolver(resolverDelegate);
    }
    return resolverDelegate;
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
}
