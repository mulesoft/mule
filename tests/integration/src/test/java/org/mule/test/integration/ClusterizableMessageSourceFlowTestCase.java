/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.connector.SchedulerController;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.context.notification.ClusterNodeNotification;
import org.mule.runtime.core.source.ClusterizableMessageSourceWrapper;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ClusterizableMessageSourceFlowTestCase extends AbstractIntegrationTestCase {

  public ClusterizableMessageSourceFlowTestCase() {
    setStartContext(false);
  }

  @Override
  protected String getConfigFile() {
    return "clusterizable-message-source-flow-config.xml";
  }

  @Test
  public void startsWhenPrimaryNode() throws Exception {
    muleContext.start();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://testOut", RECEIVE_TIMEOUT).getRight().get();
    assertEquals("TEST", response.getPayload().getValue());
  }

  @Test
  public void doesNotStartsWhenSecondaryNode() throws Exception {
    TestSchedulerController pollingController = new TestSchedulerController();
    ((DefaultMuleContext) muleContext).setSchedulerController(pollingController);
    muleContext.start();

    Flow test1 = muleContext.getRegistry().get("test1");
    ClusterizableMessageSourceWrapper messageSource = (ClusterizableMessageSourceWrapper) test1.getMessageSource();
    assertTrue(test1.getLifecycleState().isStarted());
    assertTrue(messageSource.isStarted());
  }

  @Test
  public void startsWhenNodeBecomePrimary() throws Exception {
    TestSchedulerController pollingController = new TestSchedulerController();
    ((DefaultMuleContext) muleContext).setSchedulerController(pollingController);

    muleContext.start();

    Flow test1 = (Flow) muleContext.getRegistry().get("test1");
    ClusterizableMessageSourceWrapper messageSource = (ClusterizableMessageSourceWrapper) test1.getMessageSource();

    messageSource.initialise();

    pollingController.isPrimary = true;
    muleContext.fireNotification(new ClusterNodeNotification("primary", ClusterNodeNotification.PRIMARY_CLUSTER_NODE_SELECTED));

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://testOut", RECEIVE_TIMEOUT).getRight().get();
    assertEquals("TEST", response.getPayload().getValue());
  }

  private class TestSchedulerController implements SchedulerController {

    boolean isPrimary;

    @Override
    public boolean isPrimarySchedulingInstance() {
      return isPrimary;
    }
  }
}
