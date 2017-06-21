/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
