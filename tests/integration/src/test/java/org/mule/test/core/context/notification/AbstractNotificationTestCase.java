/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.junit.Assert.fail;

import org.junit.After;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Iterator;

/**
 * Tests must define a "notificationLogger" listener
 */
public abstract class AbstractNotificationTestCase extends AbstractIntegrationTestCase {

  private NotificationLogger notificationLogger;

  @After
  public void clearNotifications() {
    if (notificationLogger != null) {
      notificationLogger.getNotifications().clear();
    }
  }

  public final void assertNotifications() throws Exception {
    notificationLogger = muleContext.getRegistry().lookupObject("notificationLogger");

    // Need to explicitly dispose manager here to get disposal notifications
    muleContext.dispose();
    // allow shutdown to complete (or get concurrent mod errors and/or miss
    // notifications)

    PollingProber prober = new PollingProber(30000, 2000);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        String notificationsLog = buildLogNotifications();
        RestrictedNode spec = getSpecification();
        validateSpecification(spec);
        assertExpectedNotifications(notificationsLog, spec);

        return true;
      }
    });
  }

  public abstract RestrictedNode getSpecification();

  public abstract void validateSpecification(RestrictedNode spec) throws Exception;

  protected String buildLogNotifications() {
    final StringBuilder logMessageBuilder = new StringBuilder();
    logMessageBuilder.append("Number of notifications: " + notificationLogger.getNotifications().size() + System.lineSeparator());
    for (Iterator<?> iterator = notificationLogger.getNotifications().iterator(); iterator.hasNext();) {
      ServerNotification notification = (ServerNotification) iterator.next();
      logMessageBuilder.append("\t" + notification + System.lineSeparator());
    }

    return logMessageBuilder.toString();
  }

  /**
   * This is destructive - do not use spec after calling this routine
   * 
   * @param notificationsLog
   */
  protected void assertExpectedNotifications(String notificationsLog, RestrictedNode spec) {
    int i = 0;
    for (Iterator<?> iterator = notificationLogger.getNotifications().iterator(); iterator.hasNext();) {
      ServerNotification notification = (ServerNotification) iterator.next();
      i++;
      int match = spec.match(notification);
      switch (match) {
        case Node.SUCCESS:
          break;
        case Node.FAILURE:
          fail("Could not match " + notification + System.lineSeparator() + notificationsLog);
          break;
        case Node.EMPTY:
          fail("Extra notification: " + notification + System.lineSeparator() + notificationsLog);
      }
    }
    if (!spec.isExhausted()) {
      fail("Specification not exhausted: " + spec.getAnyRemaining() + System.lineSeparator() + notificationsLog);
    }
  }

  protected void verifyAllNotifications(RestrictedNode spec, Class<?> clazz, int from, int to) {
    for (int action = from; action <= to; ++action) {
      if (!spec.contains(clazz, action)) {
        fail("Specification missed action " + action + " for class " + clazz);
      }
    }
  }

  protected void verifyNotification(RestrictedNode spec, Class<?> clazz, int action) {
    if (!spec.contains(clazz, action)) {
      fail("Specification missed action " + action + " for class " + clazz);
    }
  }
}
