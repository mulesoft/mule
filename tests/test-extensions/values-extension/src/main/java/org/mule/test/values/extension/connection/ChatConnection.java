/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.connection;

import static java.util.Arrays.asList;

import org.mule.runtime.api.util.MultiMap;

import java.util.List;

public class ChatConnection {

  private List<String> workspaces;
  private MultiMap<String, String> workspaceChannels;

  public ChatConnection() {
    workspaces = asList("workspace1", "workspace2", "workspace3");
    workspaceChannels = new MultiMap<>();
    workspaceChannels.put("workspace1", "one channel");
    workspaceChannels.put("workspace1", "another channel");
    workspaceChannels.put("workspace1", "last channel channel");

    workspaceChannels.put("workspace2", "channel for workspace2");
    workspaceChannels.put("workspace2", "other channel for workspace2");

    workspaceChannels.put("workspace3", "only channel for workspace3");
  }

  public List<String> getWorkspaces() {
    return workspaces;
  }

  public List<String> getChannels(String workspace) {
    return workspaceChannels.getAll(workspace);
  }

}
