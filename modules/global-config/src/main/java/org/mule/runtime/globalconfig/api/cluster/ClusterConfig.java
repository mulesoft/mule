/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.globalconfig.api.cluster;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.globalconfig.api.EnableableConfig;

/**
 * Cluster configuration settings.
 *
 * @since 4.3.0
 */
@NoImplement
public interface ClusterConfig {

  /**
   * Clustered object store configuration.
   *
   * @return the configuration.
   */
  EnableableConfig getObjectStoreConfig();

  /**
   * Clustered lock factory configuration.
   *
   * @return the configuration.
   */
  EnableableConfig getLockFactoryConfig();

  /**
   * Clustered time supplier configuration.
   *
   * @return the configuration.
   */
  EnableableConfig getTimeSupplierConfig();

  /**
   * Cluster queue manager configuration.
   *
   * @return the configuration.
   */
  EnableableConfig getQueueManager();

  /**
   * Cluster service configuration.
   *
   * @return the configuration.
   */
  EnableableConfig getClusterService();

}
