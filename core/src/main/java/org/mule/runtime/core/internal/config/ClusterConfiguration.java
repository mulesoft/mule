/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config;

/**
 * Provides the cluster configuration attributes
 */
public interface ClusterConfiguration {

  /**
   * @return the unique identifier for the cluster this mule instance belongs to
   */
  String getClusterId();

  /**
   * @return the unique identifier for this instance within the mule cluster
   */
  int getClusterNodeId();
}
