/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
