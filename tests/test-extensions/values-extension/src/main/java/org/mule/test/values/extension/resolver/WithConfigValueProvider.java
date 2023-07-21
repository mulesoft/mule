/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.test.values.extension.config.SimpleConfig;

import java.util.Set;

public class WithConfigValueProvider implements ValueProvider {

  @Config
  private SimpleConfig configuration;

  @Override
  public Set<Value> resolve() {
    return ValueBuilder.getValuesFor(configuration.getConfigValues());
  }

  @Override
  public String getId() {
    return "WithConfigValueProvider-id";
  }
}
