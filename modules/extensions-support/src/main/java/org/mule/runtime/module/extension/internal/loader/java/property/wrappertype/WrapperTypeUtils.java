/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property.wrappertype;

import static java.lang.String.format;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Optional;
import java.util.Set;
import java.util.Stack;

/**
 * Utility class to reuse logic around {@link WrapperType}
 *
 * @since 4.0
 */
public class WrapperTypeUtils {

  /**
   * Given a expression value, the expected type and the stack of {@link WrapperType wrapper types}, iterates it and
   * creates all the required {@link ValueResolver} and stacks them is necessary.
   *
   * @param expression           Expression value for the expression based value resolver.
   * @param expectedType         The expected type of the expression resolution
   * @param wrapperModelProperty Model property containing the stack of {@link WrapperType wrapper types}
   * @return The expression based {@link ValueResolver}
   */
  public static ValueResolver getExpressionBasedWrapperValueResolver(String expression, Class expectedType,
                                                                     WrapperTypesModelProperty wrapperModelProperty) {
    Stack<WrapperType> wrapperTypes = wrapperModelProperty.getWrapperTypes();
    WrapperType wrapperType = wrapperTypes.pop();
    WrapperType.ExpressionBasedResolverFactory resolverFactory = wrapperType
        .getExpressionBasedResolverFactory()
        .orElseThrow(() -> new IllegalStateException(format("Unable to create an Expression Based ValueResolver of '%s' type. No ExpressionBasedResolverFactory was registered for this type.",
                                                            wrapperType.getType().getSimpleName())));

    ValueResolver resolver = resolverFactory.getResolver(expression, expectedType);
    resolver = wrapValueResolver(resolver, wrapperTypes);
    return resolver;
  }

  /**
   * Given a static value and the stack of {@link WrapperType wrapper types}, iterates it and creates all the required
   * {@link ValueResolver} and stacks them is necessary.
   *
   * @param value                The static value
   * @param wrapperModelProperty Model property containing the stack of {@link WrapperType wrapper types}
   * @return The static {@link ValueResolver}
   */
  public static ValueResolver getStaticWrapperValueResolver(Object value, WrapperTypesModelProperty wrapperModelProperty) {
    Stack<WrapperType> wrapperTypes = wrapperModelProperty.getWrapperTypes();
    WrapperType wrapperType = wrapperTypes.pop();
    WrapperType.StaticResolverFactory resolverFactory = wrapperType
        .getStaticResolverFactory()
        .orElseThrow(() -> new IllegalStateException(format("Unable to create an Static ValueResolver of '%s' type. No StaticResolverFactory was registered for this type.",
                                                            wrapperType.getType().getSimpleName())));

    ValueResolver resolver = resolverFactory.getResolver(value);
    resolver = wrapValueResolver(resolver, wrapperTypes);
    return resolver;
  }

  /**
   * Given a static value and the stack of {@link WrapperType wrapper types}, iterates it and creates all the required
   * {@link ValueResolver} and stacks them is necessary.
   *
   * @param resolver     The {@link ValueResolver} to wrap
   * @param wrapperTypes The stack of {@link WrapperType wrapper types}
   * @return The wrapped {@link ValueResolver}
   */
  public static ValueResolver wrapValueResolver(ValueResolver resolver, Stack<WrapperType> wrapperTypes) {
    while (!wrapperTypes.empty()) {
      WrapperType delegateWrapperType = wrapperTypes.pop();
      WrapperType.DelegateResolverFactory delegateResolverFactory = delegateWrapperType
          .getDelegateResolverFactory()
          .orElseThrow(() -> new IllegalStateException(format("Unable to create a ValueResolver Wrapper of '%s' type. No DelegateResolverFactory was registered for this type.",
                                                              delegateWrapperType.getType().getSimpleName())));
      resolver = delegateResolverFactory.getResolver(resolver);
    }
    return resolver;
  }

  /**
   * Given a {@link Set} of {@link ModelProperty model properties} indicates if the enriched component
   * is considered as a wrapper type
   *
   * @param modelProperties Model properties to introspect
   * @return A boolean indicating whether the element is a wrapper type or not.
   */
  public static boolean isWrapperType(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().anyMatch(mp -> mp instanceof WrapperTypesModelProperty);
  }

  /**
   * Given a {@link Set} of {@link ModelProperty model properties} returns an {@link Optional} {@link WrapperTypesModelProperty}
   *
   * @param modelProperties Model properties to introspect
   * @return an {@link Optional} {@link WrapperTypesModelProperty}
   */
  public static Optional<WrapperTypesModelProperty> getWrapperModelProperty(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().filter(mp -> mp instanceof WrapperTypesModelProperty)
        .map(mp -> (WrapperTypesModelProperty) mp).findFirst();
  }
}
