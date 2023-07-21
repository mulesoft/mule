/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;


import static org.mule.sdk.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;
import org.mule.test.values.extension.ValueEnum;

import java.util.Set;

public class WithEnumParameterValueProvider implements ValueProvider {

  @Parameter
  private ValueEnum requiredValue;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return getValuesFor(requiredValue.name());
  }

  @Override
  public String getId() {
    return getClass().getName();
  }
}
