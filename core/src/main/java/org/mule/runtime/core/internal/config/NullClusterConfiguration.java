/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config;

/**
 * Empty cluster configuration to be used when the instance does not belong to a cluster
 */
public class NullClusterConfiguration implements ClusterConfiguration {

  @Override
  public String getClusterId() {
    return "";
  }

  @Override
  public int getClusterNodeId() {
    return 0;
  }
}
