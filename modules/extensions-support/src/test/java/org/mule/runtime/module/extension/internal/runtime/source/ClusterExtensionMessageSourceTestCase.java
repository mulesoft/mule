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
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.createMockLogger;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.setLogger;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.verifyLogMessage;
import static org.slf4j.event.Level.DEBUG;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.api.notification.ClusterNodeNotification;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;

import java.util.ArrayList;
import java.util.List;

public class ClusterExtensionMessageSourceTestCase extends AbstractExtensionMessageSourceTestCase {

  protected List<String> debugMessages;
  private Logger oldLogger;

  public ClusterExtensionMessageSourceTestCase() {
    primaryNodeOnly = true;
    SimpleRetryPolicyTemplate template = new SimpleRetryPolicyTemplate(0, 2);
    template.setNotificationFirer(notificationDispatcher);
    this.retryPolicyTemplate = template;
  }

  @Before
  public void setUpLogger() throws Exception {
    debugMessages = new ArrayList<>();
    oldLogger = setLogger(messageSource, LOGGER_FIELD_NAME, createMockLogger(debugMessages, DEBUG));
  }

  @After
  public void restoreLogger() throws Exception {
    setLogger(messageSource, LOGGER_FIELD_NAME, oldLogger);
  }

  @Override
  public void before() throws Exception {
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_CLUSTER_SERVICE, new TestClusterService());
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

  @Test
  public void dontStartIfNotPrimaryNodeLogMessage() throws Exception {
    messageSource.initialise();
    messageSource.start();
    verifyLogMessage(debugMessages,
                     "Message source 'source' on flow 'appleFlow' cannot initialize. This Message source can only run on the primary node of the cluster");
  }

  @Test
  public void startWhenPrimaryNodeLogMessage() throws Exception {
    dontStartIfNotPrimaryNode();

    muleContext.getNotificationManager()
        .fireNotification(new ClusterNodeNotification("you're up", PRIMARY_CLUSTER_NODE_SELECTED));
    verifyLogMessage(debugMessages,
                     "Message source 'source' on flow 'appleFlow' is initializing because the node became cluster's primary.");
  }

  private static class TestClusterService implements ClusterService {

    @Override
    public boolean isPrimaryPollingInstance() {
      return false;
    }

  }
}
