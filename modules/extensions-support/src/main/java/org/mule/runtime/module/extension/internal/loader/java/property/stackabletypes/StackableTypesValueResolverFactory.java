/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Optional;
import java.util.Stack;
import java.util.function.Supplier;

/**
 * Factory of {@link ValueResolver} for {@link StackableType stackable types} parameters.
 *
 * @since 4.0
 */
public class StackableTypesValueResolverFactory {

  private Supplier<Stack<StackableType>> types;

  StackableTypesValueResolverFactory(Stack<StackableType> stackableTypes) {
    checkArgument(!stackableTypes.empty(), "Stackable Types cannot be empty");
    this.types = () -> {
      Stack<StackableType> stack = new Stack<>();
      stack.addAll(stackableTypes);
      return stack;
    };
  }

  /**
   * Given a expression value, the expected type and the stack of {@link StackableType wrapper types}, iterates it and
   * creates all the required {@link ValueResolver} and stacks them is necessary.
   *
   * @param expression   Expression value for the expression based value resolver.
   * @param expectedType The expected type of the expression resolution
   * @return The expression based {@link ValueResolver}
   */
  public ValueResolver getExpressionBasedValueResolver(String expression, Class expectedType) {
    Stack<StackableType> stackableTypes = types.get();
    StackableType stackableType = stackableTypes.pop();
    StackableType.ExpressionBasedResolverFactory resolverFactory = stackableType
        .getExpressionBasedResolverFactory()
        .orElseThrow(() -> new IllegalStateException(format("Unable to create an Expression Based ValueResolver of '%s' type. No ExpressionBasedResolverFactory was registered for this type.",
                                                            stackableType.getType().getSimpleName())));

    ValueResolver resolver = resolverFactory.getResolver(expression, expectedType);
    resolver = getWrapperValueResolver(resolver, stackableTypes);
    return resolver;
  }


  /**
   * Given a static value and the stack of {@link StackableType wrapper types}, iterates it and creates all the required
   * {@link ValueResolver} and stacks them is necessary.
   *
   * @param value The static value
   * @return The static {@link ValueResolver}
   */
  public ValueResolver getStaticValueResolver(Object value) {
    Stack<StackableType> stackableTypes = types.get();
    StackableType stackableType = stackableTypes.pop();
    StackableType.StaticResolverFactory resolverFactory = stackableType
        .getStaticResolverFactory()
        .orElseThrow(() -> new IllegalStateException(format("Unable to create an Static ValueResolver of '%s' type. No StaticResolverFactory was registered for this type.",
                                                            stackableType.getType().getSimpleName())));

    ValueResolver resolver = resolverFactory.getResolver(value);
    resolver = getWrapperValueResolver(resolver, stackableTypes);
    return resolver;
  }

  /**
   * Given a static value, the stack of {@link StackableType stacked types} and the desired class for the static resolver, if
   * the class is not found as the root of the parameter type, an {@link Optional#empty()} will be returned.
   * Also, iterates it and creates all the required {@link ValueResolver} and stacks them is necessary.
   *
   * @param value The static value
   * @return The static {@link ValueResolver}
   */
  public Optional<ValueResolver> getStaticValueResolver(Object value, Class clazz) {
    Stack<StackableType> stackableTypes = types.get();
    StackableType stackableType = stackableTypes.get(stackableTypes.size() - 1);
    if (stackableType.getType().equals(clazz)) {
      return of(getStaticValueResolver(value));
    }
    return empty();
  }

  /**
   * Given a static value and the stack of {@link StackableType wrapper types}, iterates it and creates all the required
   * {@link ValueResolver} and stacks them is necessary.
   *
   * @param resolverDelegate The {@link ValueResolver} to wrap
   * @return The wrapped {@link ValueResolver}
   */
  public ValueResolver getWrapperValueResolver(ValueResolver resolverDelegate) {
    return getWrapperValueResolver(resolverDelegate, types.get());
  }

  private ValueResolver getWrapperValueResolver(ValueResolver resolver, Stack<StackableType> types) {
    while (!types.empty()) {
      StackableType delegateStackableType = types.pop();
      StackableType.DelegateResolverFactory delegateResolverFactory = delegateStackableType
          .getDelegateResolverFactory()
          .orElseThrow(() -> new IllegalStateException(format("Unable to create a ValueResolver Wrapper of '%s' type. No DelegateResolverFactory was registered for this type.",
                                                              delegateStackableType.getType().getSimpleName())));
      resolver = delegateResolverFactory.getResolver(resolver);
    }
    return resolver;
  }

}
