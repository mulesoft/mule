/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.test.values.extension.ValuesConnection;

import java.util.Set;

public class WithConnectionValueProvider implements ValueProvider {

  @org.mule.sdk.api.annotation.param.Connection
  ValuesConnection connection;

  @Override
  public Set<Value> resolve() {
    return ValueBuilder.getValuesFor(connection.getEntities());
  }

  @Override
  public String getId() {
    return "Value provider with connection";
  }
}
