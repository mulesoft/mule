/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
