/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
