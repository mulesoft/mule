/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config.params;

import static java.util.Collections.emptyMap;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Map;

public class EmptyParameterValueResolver implements ParameterValueResolver {

  private static final ParameterValueResolver instance = new EmptyParameterValueResolver();

  static ParameterValueResolver emptyParameterValueResolver() {
    return instance;
  }

  private EmptyParameterValueResolver() {}

  @Override
  public Object getParameterValue(String parameterName) throws ValueResolvingException {
    return null;
  }

  @Override
  public Map<String, ValueResolver<? extends Object>> getParameters() throws ValueResolvingException {
    return emptyMap();
  }
}
