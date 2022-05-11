/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.sdk.api.values.ValueBuilder.newValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.test.values.extension.connection.ChatConnection;

public class ChatMultiLevelValueProvider implements ValueProvider {

  @Connection
  private ChatConnection chatConnection;

  @Override
  public Set<Value> resolve() {
    Set<Value> values = new HashSet<>();
    List<String> workspaces = chatConnection.getWorkspaces();
    workspaces.stream().forEach(workspace -> {
      ValueBuilder valueBuilder = newValue(workspace);
      chatConnection.getChannels(workspace).stream().forEach(channel -> valueBuilder.withChild(newValue(channel)));
      values.add(valueBuilder.build());
    });

    return values;
  }

  @Override
  public String getId() {
    return "Chat multilevel value provider";
  }
}
