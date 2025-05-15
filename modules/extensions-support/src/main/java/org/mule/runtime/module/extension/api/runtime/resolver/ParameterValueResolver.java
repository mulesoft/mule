/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.resolver;

import static java.util.stream.Collectors.toMap;

import org.mule.runtime.module.extension.internal.runtime.resolver.StaticParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;

import java.util.Map;

/**
 * Contract for extension components that knows how to resolve parameter values
 *
 * @since 4.0
 */
public interface ParameterValueResolver {

  /**
   * Creates a resolver for the provided {@code parameters}.
   *
   * @param parameters the parameters to create a resolver for.
   * @return a new resolver
   */
  public static ParameterValueResolver staticParametersFrom(Map<String, ?> parameters) {
    return new StaticParameterValueResolver(parameters.entrySet().stream()
        .collect(toMap(Map.Entry::getKey, e -> new StaticValueResolver<>(e.getValue()))));
  }

  /**
   * @return The parameter value
   * @throws ValueResolvingException if the resolution fails
   */
  Object getParameterValue(String parameterName) throws ValueResolvingException;

  /**
   * @return a map with all the parameter names as keys and their respective {@link ValueResolver}s as values.
   *
   * @since 4.2.3
   */
  Map<String, ValueResolver<? extends Object>> getParameters() throws ValueResolvingException;
}
