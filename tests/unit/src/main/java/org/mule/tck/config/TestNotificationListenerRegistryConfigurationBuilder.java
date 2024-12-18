/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.config;

import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTED;

import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationListenerRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;

public class TestNotificationListenerRegistryConfigurationBuilder extends AbstractConfigurationBuilder {

  private Latch contextStartedLatch;

  public TestNotificationListenerRegistryConfigurationBuilder(Latch contextStartedLatch) {
    this.contextStartedLatch = contextStartedLatch;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws RegistrationException {
    final DefaultNotificationListenerRegistry notificationListenerRegistry = new DefaultNotificationListenerRegistry();
    notificationListenerRegistry.setNotificationManager(muleContext.getNotificationManager());
    notificationListenerRegistry.registerListener(new MuleContextNotificationListener<>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(MuleContextNotification notification) {
        if (new IntegerAction(CONTEXT_STARTED).equals(notification.getAction())) {
          contextStartedLatch.countDown();
        }
      }
    });

    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(NotificationListenerRegistry.REGISTRY_KEY,
                                                                         notificationListenerRegistry);
  }
}
