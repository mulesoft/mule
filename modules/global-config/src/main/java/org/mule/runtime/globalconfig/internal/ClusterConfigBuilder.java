/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig.internal;

import static org.mule.runtime.globalconfig.internal.DefaultEnableableConfig.ENABLED_PROPERTY;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.globalconfig.api.EnableableConfig;
import org.mule.runtime.globalconfig.api.cluster.ClusterConfig;
import org.mule.runtime.globalconfig.api.exception.RuntimeGlobalConfigException;

import com.typesafe.config.Config;

/**
 * Internal builder for {@link ClusterConfig}.
 */
public class ClusterConfigBuilder {

  /**
   * @return the default configuration for {@link ClusterConfig}
   */
  public static ClusterConfig defaultClusterConfig() {
    ClusterConfigImpl clusterConfig = new ClusterConfigImpl();
    clusterConfig.lockFactoryConfig = new DefaultEnableableConfig(true);
    clusterConfig.objectStoreConfig = new DefaultEnableableConfig(true);
    clusterConfig.timeSupplierConfig = new DefaultEnableableConfig(true);
    clusterConfig.queueManagerConfig = new DefaultEnableableConfig(true);
    clusterConfig.clusterServiceConfig = new DefaultEnableableConfig(true);
    return clusterConfig;
  }

  /**
   * @param mavenConfig the maven configuration set by the user
   * @return a {@link MavenConfiguration} created by using the user configuration and default values set by mule.
   */
  public static ClusterConfig parseClusterConfig(Config mavenConfig) {
    ClusterConfigImpl clusterConfig = new ClusterConfigImpl();
    try {
      clusterConfig.objectStoreConfig = parseEnabledConfig(mavenConfig, "objectStore");
      clusterConfig.lockFactoryConfig = parseEnabledConfig(mavenConfig, "lockFactory");
      clusterConfig.timeSupplierConfig = parseEnabledConfig(mavenConfig, "timeSupplier");
      clusterConfig.queueManagerConfig = parseEnabledConfig(mavenConfig, "queueManager");
      clusterConfig.clusterServiceConfig = parseEnabledConfig(mavenConfig, "clusterService");
      return clusterConfig;
    } catch (Exception e) {
      if (e instanceof RuntimeGlobalConfigException) {
        throw e;
      }
      throw new RuntimeGlobalConfigException(e);
    }
  }

  private static DefaultEnableableConfig parseEnabledConfig(Config clusterConfig, String propertyName) {
    Config enabledConfig = clusterConfig.hasPath(propertyName) ? clusterConfig.getConfig(propertyName) : null;
    return enabledConfig == null ? new DefaultEnableableConfig(true)
        : new DefaultEnableableConfig(enabledConfig.hasPath(ENABLED_PROPERTY) ? enabledConfig.getBoolean(ENABLED_PROPERTY)
            : true);
  }

  public static class ClusterConfigImpl implements ClusterConfig {

    private EnableableConfig objectStoreConfig;
    private EnableableConfig lockFactoryConfig;
    private EnableableConfig timeSupplierConfig;
    private EnableableConfig queueManagerConfig;
    private EnableableConfig clusterServiceConfig;

    @Override
    public EnableableConfig getObjectStoreConfig() {
      return objectStoreConfig;
    }

    @Override
    public EnableableConfig getLockFactoryConfig() {
      return lockFactoryConfig;
    }

    @Override
    public EnableableConfig getTimeSupplierConfig() {
      return timeSupplierConfig;
    }

    @Override
    public EnableableConfig getQueueManager() {
      return queueManagerConfig;
    }

    @Override
    public EnableableConfig getClusterService() {
      return clusterServiceConfig;
    }
  }

}
