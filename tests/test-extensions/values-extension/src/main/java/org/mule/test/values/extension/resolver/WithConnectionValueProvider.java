/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
