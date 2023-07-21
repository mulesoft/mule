/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;

import java.util.Set;

public class WithRequiredParameterFromGroupValueProvider implements ValueProvider {

  @Parameter
  String anyParameter;

  @Override
  public Set<Value> resolve() {
    return ValueBuilder.getValuesFor("anyParameter:" + anyParameter);
  }

  @Override
  public String getId() {
    return "WithRequiredParameterFromGroupValueProvider-id";
  }
}
