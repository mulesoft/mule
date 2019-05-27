/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.notification.ClusterNodeNotification.PRIMARY_CLUSTER_NODE_SELECTED;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_SERVICE;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;

import org.junit.Test;
import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.api.notification.ClusterNodeNotification;

public class ClusterExtensionMessageSourceTestCase extends AbstractExtensionMessageSourceTestCase {

  public ClusterExtensionMessageSourceTestCase() {
    primaryNodeOnly = true;
  }

  @Override
  public void before() throws Exception {
    registerObject(muleContext, OBJECT_CLUSTER_SERVICE, new TestClusterService());
    super.before();
  }

  @Override
  protected SourceAdapter createSourceAdapter() {
    return spy(super.createSourceAdapter());
  }

  @Test
  public void dontStartIfNotPrimaryNode() throws Exception {
    messageSource.initialise();
    messageSource.start();

    verify(sourceAdapter, never()).initialise();
    verify(sourceAdapter, never()).start();
  }

  @Test
  public void startWhenPrimaryNode() throws Exception {
    dontStartIfNotPrimaryNode();

    muleContext.getNotificationManager()
        .fireNotification(new ClusterNodeNotification("you're up", PRIMARY_CLUSTER_NODE_SELECTED));
    verify(sourceAdapter, atLeastOnce()).initialise();
    verify(sourceAdapter, times(1)).start();
  }

  private static class TestClusterService implements ClusterService {

    @Override
    public boolean isPrimaryPollingInstance() {
      return false;
    }

  }
}
