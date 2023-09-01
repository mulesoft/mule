/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;

import java.util.Set;

public class WithRequiredParametersFromConfigValueProvider implements ValueProvider {

  @Parameter
  private String required1;

  @Parameter
  private String required2;

  @Override
  public Set<Value> resolve() {
    return ValueBuilder.getValuesFor("required1:" + required1, "required2:" + required2);
  }

  @Override
  public String getId() {
    return "WithRequiredParametersFromConfigValueProvider-Id";
  }
}
