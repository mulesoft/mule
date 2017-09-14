/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes;

import static java.util.Optional.ofNullable;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Optional;

/**
 * Represents a certain type which is the generic wrapper of another one.
 *
 * @since 4.0
 */
public class StackableType {

  private final Class type;
  private final ExpressionBasedResolverFactory expressionBasedResolverFactory;
  private final StaticResolverFactory staticResolverFactory;
  private final DelegateResolverFactory delegateResolverFactory;

  /**
   * Creates a new instance.
   * If a factory is not provided it will be considered that the correspondent type doesn't support {@link ValueResolver}
   * of the factory type.
   *
   * @param type                           The represented type
   * @param expressionBasedResolverFactory A factory that creates instances of expression based {@link ValueResolver value resolvers}
   * @param staticResolverFactory          A factory that creates instances of static {@link ValueResolver value resolvers}
   * @param delegateResolverFactory        A factory that create instances of {@link ValueResolver value resolver} wrappers
   */
  private StackableType(Class type, ExpressionBasedResolverFactory expressionBasedResolverFactory,
                        StaticResolverFactory staticResolverFactory, DelegateResolverFactory delegateResolverFactory) {
    this.type = type;
    this.expressionBasedResolverFactory = expressionBasedResolverFactory;
    this.staticResolverFactory = staticResolverFactory;
    this.delegateResolverFactory = delegateResolverFactory;
  }

  /**
   * @return The {@link Class} of the wrapper type
   */
  public Class getType() {
    return type;
  }

  /**
   * @return An {@link Optional} {@link ExpressionBasedResolverFactory}, if {@link Optional#empty()} it means that
   * the current type doesn't support this kind of {@link ValueResolver}
   */
  Optional<ExpressionBasedResolverFactory> getExpressionBasedResolverFactory() {
    return ofNullable(expressionBasedResolverFactory);
  }

  /**
   * @return An {@link Optional} {@link StaticResolverFactory}, if {@link Optional#empty()} it means that
   * the current type doesn't support this kind of {@link ValueResolver}
   */
  Optional<StaticResolverFactory> getStaticResolverFactory() {
    return ofNullable(staticResolverFactory);
  }

  /**
   * @return An {@link Optional} {@link DelegateResolverFactory}, if {@link Optional#empty()} it means that
   * the current type doesn't support this kind of {@link ValueResolver}
   */
  Optional<DelegateResolverFactory> getDelegateResolverFactory() {
    return ofNullable(delegateResolverFactory);
  }

  /**
   * Creates a new instance of {@link Builder Wrapper Type Builder}
   *
   * @param type The type that the {@link StackableType} will represent
   * @return The builder
   */
  public static <T> Builder<T> builder(Class<T> type) {
    return new Builder<>(type);
  }

  public static class Builder<T> {

    private Class<T> type;
    private ExpressionBasedResolverFactory<T> expressionBasedResolverFactory;
    private StaticResolverFactory staticResolverFactory;
    private DelegateResolverFactory delegateResolverFactory;

    private Builder(Class<T> type) {
      this.type = type;
    }

    public Builder<T> setExpressionBasedResolverFactory(ExpressionBasedResolverFactory<T> expressionBasedResolverFactory) {
      this.expressionBasedResolverFactory = expressionBasedResolverFactory;
      return this;
    }

    public Builder<T> setStaticResolverFactory(StaticResolverFactory<T> staticResolverFactory) {
      this.staticResolverFactory = staticResolverFactory;
      return this;
    }

    public Builder<T> setDelegateResolverFactory(DelegateResolverFactory<T> delegateResolverFactory) {
      this.delegateResolverFactory = delegateResolverFactory;
      return this;
    }

    public StackableType build() {
      return new StackableType(type, expressionBasedResolverFactory, staticResolverFactory, delegateResolverFactory);
    }
  }


  /**
   * Factory for expression based {@link ValueResolver}
   *
   * @param <T> The type of values that the {@link ValueResolver} will return
   */
  @FunctionalInterface
  public interface ExpressionBasedResolverFactory<T> {

    ValueResolver<T> getResolver(String value, Class<T> expectedType);
  }

  /**
   * Factory for static {@link ValueResolver}
   *
   * @param <T> The type of values that the {@link ValueResolver} will return
   */
  @FunctionalInterface
  public interface StaticResolverFactory<T> {

    ValueResolver<T> getResolver(Object value);
  }

  /**
   * Factory for {@link ValueResolver} wrappers
   *
   * @param <T> The type of values that the {@link ValueResolver} will return
   */
  @FunctionalInterface
  public interface DelegateResolverFactory<T> {

    ValueResolver<T> getResolver(ValueResolver delegate);
  }
}

