/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.context.notification.ExceptionStrategyNotification.PROCESS_END;
import static org.mule.runtime.core.context.notification.ExceptionStrategyNotification.PROCESS_START;

import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class ExceptionStrategyNotificationTestCase extends AbstractNotificationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();


  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/exception-strategy-notification-test-flow.xml";
  }

  @Override
  public void doTest() throws Exception {
    assertNotNull(flowRunner("catch-es").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("choice-es").withPayload(TEST_PAYLOAD).run());
    expectedException.expect(ComponentException.class);
    assertNotNull(flowRunner("rollback-es").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("default-es").withPayload(TEST_PAYLOAD).run());
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node().serial(node(PROCESS_START).serial(node(PROCESS_END))).serial(node(PROCESS_START).serial(node(PROCESS_END)))
        .serial(node(PROCESS_START).serial(node(PROCESS_END))).serial(node(PROCESS_START).serial(node(PROCESS_END)));
  }

  private RestrictedNode node(int action) {
    return new Node(ExceptionStrategyNotification.class, action);
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {}
}
