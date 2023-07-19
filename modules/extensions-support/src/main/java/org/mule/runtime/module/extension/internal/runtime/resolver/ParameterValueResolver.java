/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;

import java.util.Map;

/**
 * Contract for extension components that knows how to resolve parameter values
 *
 * @since 4.0
 */
public interface ParameterValueResolver {

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
