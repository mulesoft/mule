/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getClusterConfig;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.test.allure.AllureConstants.RuntimeGlobalConfiguration.MavenGlobalConfiguration.MAVEN_GLOBAL_CONFIGURATION_STORY;
import static org.mule.test.allure.AllureConstants.RuntimeGlobalConfiguration.RUNTIME_GLOBAL_CONFIGURATION;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.globalconfig.api.cluster.ClusterConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableList;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@Feature(RUNTIME_GLOBAL_CONFIGURATION)
@Story(MAVEN_GLOBAL_CONFIGURATION_STORY)
public class ClusterConfigTestCase extends AbstractMuleTestCase {

  private static final String OBJECT_STORE = "objectStore";
  private static final String LOCK_FACTORY = "lockFactory";
  private static final String TIME_SUPPLIER = "timeSupplier";
  private static final String QUEUE_MANAGER = "queueManager";
  private static final String CLUSTER_SERVICE = "clusterService";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Description("Test a single file loaded from the classpath and verifies that the mule.conf and mule.properties json are not taken into account.")
  @Test
  public void clusterConfigurationWithDefaultSettings() throws Exception {
    testWithSystemProperty("mule.configFile", "mule-config-empty", () -> {
      testClusterConfiguration(true, true, true, true, true);
    });
  }

  @Description("Test a single file loaded from the classpath and verifies that the mule.conf and mule.properties json are not taken into account.")
  @Test
  public void clusterConfigurationAllConfigured() {
    testClusterConfiguration(false, false, false, false, false);
  }

  @Description("Disable single service with property")
  @Test
  public void serviceDisabledWithProperty() throws Exception {
    for (String service : ImmutableList.of(OBJECT_STORE, LOCK_FACTORY, TIME_SUPPLIER, QUEUE_MANAGER, CLUSTER_SERVICE)) {
      testWithSystemProperty(String.format("muleRuntimeConfig.cluster.%s.enabled", service), "true", () -> {
        testClusterConfiguration("objectStore".equals(service), "lockFactory".equals(service), "timeSupplier".equals(service),
                                 "queueManager".equals(service), "clusterService".equals(service));
      });
    }
  }

  private void testClusterConfiguration(boolean objectStoreEnabled, boolean lockFactoryEnabled, boolean timeSupplierEnabled,
                                        boolean queueManagerEnabled, boolean clusterServiceEnabled) {
    GlobalConfigLoader.reset();
    ClusterConfig clusterConfig = getClusterConfig();
    assertThat(clusterConfig.getLockFactoryConfig().isEnabled(), is(lockFactoryEnabled));
    assertThat(clusterConfig.getObjectStoreConfig().isEnabled(), is(objectStoreEnabled));
    assertThat(clusterConfig.getTimeSupplierConfig().isEnabled(), is(timeSupplierEnabled));
    assertThat(clusterConfig.getQueueManager().isEnabled(), is(queueManagerEnabled));
    assertThat(clusterConfig.getClusterService().isEnabled(), is(clusterServiceEnabled));
  }

}
