/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingException;

import java.util.HashMap;
import java.util.Map;

public class StaticParameterValueResolver implements ParameterValueResolver {

  private final Map<String, ValueResolver<?>> resolvedParameters;

  public StaticParameterValueResolver(Map<String, ValueResolver<?>> resolvedParameters) {
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
