/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.notification.ClusterNodeNotification.PRIMARY_CLUSTER_NODE_SELECTED;
import org.mule.runtime.api.notification.ClusterNodeNotification;
import org.mule.runtime.core.internal.connector.SchedulerController;
import org.mule.runtime.core.internal.context.DefaultMuleContext;

import org.junit.Test;
import org.mockito.verification.VerificationMode;

public class ClusterExtensionMessageSourceTestCase extends AbstractExtensionMessageSourceTestCase {

  public ClusterExtensionMessageSourceTestCase() {
    primaryNodeOnly = true;
  }

  @Override
  public void before() throws Exception {
    super.before();

    SchedulerController schedulerController = mock(SchedulerController.class);
    when(schedulerController.isPrimarySchedulingInstance()).thenReturn(false);
    ((DefaultMuleContext) muleContext).setSchedulerController(schedulerController);
  }

  @Override
  protected SourceAdapter createSourceAdapter() {
    return spy(super.createSourceAdapter());
  }

  @Test
  public void dontStartIfNotPrimaryNode() throws Exception {
    messageSource.initialise();
    messageSource.start();

    assertStarted(never());
  }

  @Test
  public void startWhenPrimaryNode() throws Exception {
    dontStartIfNotPrimaryNode();

    muleContext.getNotificationManager()
        .fireNotification(new ClusterNodeNotification("you're up", PRIMARY_CLUSTER_NODE_SELECTED));
    assertStarted(times(1));
  }

  private void assertStarted(VerificationMode mode) throws Exception {
    verify(sourceAdapter, mode).initialise();
    verify(sourceAdapter, mode).start();
  }
}
