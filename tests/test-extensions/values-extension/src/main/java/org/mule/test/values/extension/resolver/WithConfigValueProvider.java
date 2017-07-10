/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.test.values.extension.config.SimpleConfig;

import java.util.Set;

public class WithConfigValueProvider implements ValueProvider {

  @Config
  private SimpleConfig configuration;

  @Override
  public Set<Value> resolve() {
    return ValueBuilder.getValuesFor(configuration.getConfigValues());
  }

}
