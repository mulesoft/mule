/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.cluster;

import org.mule.runtime.api.cluster.ClusterService;

/**
 * Implementation of {@link ClusterService} to be used when the runtime is not in cluster mode.
 * 
 * @since 4.0
 */
public class DefaultClusterService implements ClusterService {

  @Override
  public boolean isPrimaryPollingInstance() {
    return true;
  }
}
