/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import java.util.Set;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.test.values.extension.connection.ChatConnection;

public class WorkspacesValueProvider implements ValueProvider {

  @Connection
  private ChatConnection chatConnection;

  @Override
  public Set<Value> resolve() {
    return ValueBuilder.getValuesFor(chatConnection.getWorkspaces());
  }

  @Override
  public String getId() {
    return "Workspaces value provider";
  }
}
