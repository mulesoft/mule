/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.junit.Assert.assertNull;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.component.ComponentException;
import org.mule.functional.listener.ExceptionListener;
import org.mule.runtime.core.context.notification.ExceptionNotification;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class ExceptionNotificationTestCase extends AbstractNotificationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/exception-notification-test-flow.xml";
  }

  @Override
  public void doTest() throws Exception {
    ExceptionListener exceptionListener = new ExceptionListener(muleContext);
    expectedException.expect(ComponentException.class);
    MuleMessage result = flowRunner("the-service").withPayload(getTestMuleMessage()).run().getMessage();
    // processing is async, give time for the exception notificator to run
    exceptionListener.waitUntilAllNotificationsAreReceived();

    assertNull(result);
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node(ExceptionNotification.class, ExceptionNotification.EXCEPTION_ACTION);
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {
    verifyAllNotifications(spec, ExceptionNotification.class, ExceptionNotification.EXCEPTION_ACTION,
                           ExceptionNotification.EXCEPTION_ACTION);
  }
}
