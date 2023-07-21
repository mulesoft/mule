/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

public class WithReservedNameActingParameterValueProvider implements ValueProvider {

  @Parameter
  private String type;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return getValuesFor(type);
  }
}
