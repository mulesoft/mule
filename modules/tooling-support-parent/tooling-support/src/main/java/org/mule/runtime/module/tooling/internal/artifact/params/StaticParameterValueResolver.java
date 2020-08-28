/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.params;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.HashMap;
import java.util.Map;

public class StaticParameterValueResolver implements ParameterValueResolver {

  private final Map<String, ValueResolver<?>> resolvedParameters;

  static ParameterValueResolver multipleParametersStaticResolver(Map<String, ?> parameters) {
    return new StaticParameterValueResolver(parameters.entrySet().stream()
        .collect(toMap(Map.Entry::getKey, e -> new StaticValueResolver<>(e.getValue()))));
  }

  private StaticParameterValueResolver(Map<String, ValueResolver<?>> resolvedParameters) {
    this.resolvedParameters = new HashMap<>(resolvedParameters);
  }

  @Override
  public Object getParameterValue(String parameterName) throws ValueResolvingException {
    if (resolvedParameters.containsKey(parameterName)) {
      try {
        return resolvedParameters.get(parameterName).resolve(null);
      } catch (MuleException e) {
        throw new ValueResolvingException("Could not resolve value for " + parameterName, e);
      }
    }
    return null;
  }

  @Override
  public Map<String, ValueResolver<? extends Object>> getParameters() throws ValueResolvingException {
    return unmodifiableMap(resolvedParameters);
  }
}
