/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;

import java.util.Set;

public class WithRequiredParametersFromConfigLegacyValueProvider implements ValueProvider {

  @Parameter
  private String required1;

  @Parameter
  private String required2;

  @Override
  public Set<Value> resolve() {
    return ValueBuilder.getValuesFor("required1:" + required1, "required2:" + required2);
  }

}
