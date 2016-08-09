/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.junit.Assert.assertNotNull;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.context.notification.ComponentMessageNotification;

/**
 * Test ComponentNotifications/Listeners by sending events to a component. A pre and post notification should be received by
 * listeners.
 */
public class ComponentMessageNotificationTestCase extends AbstractNotificationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/component-message-notification-test-flow.xml";
  }

  @Override
  public void doTest() throws Exception {
    MuleClient client = muleContext.getClient();
    assertNotNull(flowRunner("service-1").withPayload("hello sweet world").run());
    flowRunner("service-2").withPayload("goodbye cruel world").run();
    assertNotNull(client.request("test://out-2", RECEIVE_TIMEOUT));
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node().parallel(new Node(ComponentMessageNotification.class, ComponentMessageNotification.COMPONENT_PRE_INVOKE))
        .parallel(new Node(ComponentMessageNotification.class, ComponentMessageNotification.COMPONENT_POST_INVOKE))
        .parallel(new Node(ComponentMessageNotification.class, ComponentMessageNotification.COMPONENT_PRE_INVOKE))
        .parallel(new Node(ComponentMessageNotification.class, ComponentMessageNotification.COMPONENT_POST_INVOKE));
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {
    verifyAllNotifications(spec, ComponentMessageNotification.class, ComponentMessageNotification.COMPONENT_PRE_INVOKE,
                           ComponentMessageNotification.COMPONENT_POST_INVOKE);
  }
}
