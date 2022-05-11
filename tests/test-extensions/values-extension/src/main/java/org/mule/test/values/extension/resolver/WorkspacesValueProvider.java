/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
