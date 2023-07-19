/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleException;

import javax.inject.Inject;

/**
 * An {@link LifecycleAwareValueResolverWrapper} which throws an {@link IllegalArgumentException} if the resolved value is
 * {@code null}.
 * <p>
 * This wrapper is intended to be used on parameters which have been assigned to an expression, but we want to prevent that
 * expression from evaluating to {@code null}
 *
 * @param <T> the generic type of the resolved values
 * @since 4.0
 */
public class RequiredParameterValueResolverWrapper<T> extends LifecycleAwareValueResolverWrapper<T> {

  @Inject
  private ConfigurationProperties properties;

  private final String errorMessage;

  /**
   * Creates a new instance
   *
   * @param delegate      the wrapped {@link ValueResolver}
   * @param parameterName the name of the parameter this resolver is associated to
   * @param literalValue  the evaluated expression
   */
  public RequiredParameterValueResolverWrapper(ValueResolver<T> delegate,
                                               String parameterName,
                                               String literalValue) {
    super(delegate);
    this.errorMessage = format("Required parameter '%s' was assigned with value '%s' which resolved to null." +
        " Required parameters need to be assigned with non null values",
                               parameterName, literalValue);
  }

  /**
   * Creates a new instance
   *
   * @param delegate      the wrapped {@link ValueResolver}
   * @param parameterName the name of the parameter this resolver is associated to
   */
  public RequiredParameterValueResolverWrapper(ValueResolver<T> delegate,
                                               String parameterName) {
    super(delegate);
    this.errorMessage = format("Required parameter '%s' resolved to null." +
        " Required parameters need to be assigned with non null values",
                               parameterName);
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    T value = super.resolve(context);
    if (value == null) {
      throw new IllegalArgumentException(errorMessage);
    }

    return value;
  }
}
