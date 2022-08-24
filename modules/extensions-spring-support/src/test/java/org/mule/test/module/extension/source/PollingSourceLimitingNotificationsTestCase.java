/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.lang.String.valueOf;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.notification.PollingSourceItemNotification.ITEM_REJECTED_LIMIT;
import static org.mule.runtime.api.util.MuleSystemProperties.EMIT_POLLING_SOURCE_NOTIFICATIONS;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.notification.PollingSourceItemNotification;
import org.mule.runtime.api.notification.PollingSourceItemNotificationListener;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class PollingSourceLimitingNotificationsTestCase extends AbstractExtensionFunctionalTestCase {

  private static final int TIMEOUT = 10000;
  private static final String ITEM_DISPATCHED_MESSAGE = "item dispatched to flow";
  private static final String ITEM_REJECTED_LIMIT_MESSAGE = "item rejected because it exceeded the item limit per poll";

  @Rule
  public SystemProperty emitNotifications = new SystemProperty(EMIT_POLLING_SOURCE_NOTIFICATIONS, "true");

  @Override
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }

  @Override
  protected Map<String, Object> getExtensionLoaderContextAdditionalParameters() {
    return singletonMap(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
  }

  // Since the ENABLE_POLLING_SOURCE_LIMIT_PARAMETER changes the extension model generator, we have to make the parsers cache
  // aware of this property so that each tests uses the expected parser with the expected extension model definition.
  @Override
  protected Map<String, String> artifactProperties() {
    return singletonMap(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, "true");
  }

  @Override
  protected String getConfigFile() {
    return "source/polling-source-limiting-config.xml";
  }

  @Test
  public void itemPollLimitNotifications() throws Exception {
    final Latch latch = new Latch();
    final List<PollingSourceItemNotification> sourceNotifications = new ArrayList<>();
    final PollingSourceItemNotificationListener sourceListener = notification -> {
      sourceNotifications.add(notification);
      if (valueOf(ITEM_REJECTED_LIMIT).equals(notification.getAction().getIdentifier())) {
        latch.release();
      }
    };
    notificationListenerRegistry.registerListener(sourceListener);

    try {
      startFlow("limitOneNotification");
      boolean timeout = !latch.await(TIMEOUT, MILLISECONDS);
      assertThat(timeout, is(false));
      assertThat(sourceNotifications.get(0).getActionName(), is(ITEM_DISPATCHED_MESSAGE));
      assertThat(sourceNotifications.get(1).getActionName(), is(ITEM_REJECTED_LIMIT_MESSAGE));
    } finally {
      notificationListenerRegistry.unregisterListener(sourceListener);
    }
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
