/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.sdk.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;

import java.util.List;
import java.util.Set;

public class WithListParameterValueProvider implements ValueProvider {

  @Parameter
  private List<String> requiredValue;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return getValuesFor(requiredValue);
  }

  @Override
  public String getId() {
    return getClass().getName();
  }
}
