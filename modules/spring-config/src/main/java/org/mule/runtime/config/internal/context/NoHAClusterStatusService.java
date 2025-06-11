/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import org.mule.runtime.api.cluster.ClusterState;
import org.mule.runtime.api.cluster.ClusterStatus;
import org.mule.runtime.api.cluster.ClusterStatusService;

/**
 * A {@link ClusterStatusService} implementation for non-high-availability (non-HA) environments.
 * <p>
 * This class represents a cluster status where there is only a single node, and the cluster is always considered active but not
 * safe (since there is no redundancy or failover support).
 */
public class NoHAClusterStatusService implements ClusterStatusService {

  /**
   * Returns the status of the cluster in a non-HA configuration.
   * <p>
   * The returned {@link ClusterStatus} indicates:
   * <ul>
   * <li>Cluster size is always {@code 1}.</li>
   * <li>Cluster state is always {@link ClusterState#ACTIVE}.</li>
   * <li>The cluster is not considered safe (returns {@code false}).</li>
   * </ul>
   *
   * @return a {@link ClusterStatus} representing a single-node, always-active, not-safe cluster
   */
  @Override
  public ClusterStatus getClusterStatus() {
    return new ClusterStatus() {

      @Override
      public int getClusterSize() {
        return 1;
      }

      @Override
      public ClusterState getClusterState() {
        return ClusterState.ACTIVE;
      }

      @Override
      public boolean isClusterSafe() {
        return true;
      }
    };
  }
}
