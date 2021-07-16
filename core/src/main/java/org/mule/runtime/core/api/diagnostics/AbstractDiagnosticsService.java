/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_DIAGNOSTICS_SERVICE;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.diagnostics.notification.ProfilingNotification;
import org.mule.runtime.core.api.diagnostics.notification.DefaultProfilingNotificationListener;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

/**
 * A {@link AbstractDiagnosticsService} that discovers available {@link ProfilingDataConsumer}
 *
 * @since 4.4.0
 */
public abstract class AbstractDiagnosticsService implements DiagnosticsService, Initialisable, Disposable {

  @Inject
  protected ServerNotificationManager notificationManager;

  @Inject
  private ServerNotificationHandler serverNotificationHandler;

  @Inject
  FeatureFlaggingService featureFlags;

  private final Set<NotificationListener<ProfilingNotification>> addedListeners = new HashSet<>();

  @Override
  public void initialise() throws InitialisationException {
    if (featureFlags.isEnabled(ENABLE_DIAGNOSTICS_SERVICE)) {
      registerNotificationListeners(getDiscoveryStrategy().discover());
    }
  }

  private void registerNotificationListeners(Set<ProfilingDataConsumer<ProfilingEventContext>> profilerDataConsumers) {
    profilerDataConsumers.forEach(this::registerNotificationListener);
  }

  private void registerNotificationListener(ProfilingDataConsumer<ProfilingEventContext> profilingDataConsumer) {
    NotificationListener<ProfilingNotification> profilingNotificationListener =
        new DefaultProfilingNotificationListener(profilingDataConsumer);
    notificationManager.addListenerSubscription(profilingNotificationListener, pn -> filterByAction(profilingDataConsumer, pn));
    addedListeners.add(profilingNotificationListener);
  }

  private boolean filterByAction(ProfilingDataConsumer<ProfilingEventContext> profilerDataConsumer,
                                 ProfilingNotification profilerNotification) {
    return profilerDataConsumer.profilerEventTypes().stream()
        .anyMatch(
                  eventType -> eventType.getProfilingEventName()
                      .equals(profilerNotification.getActionName()))
        &&
        profilerDataConsumer.selector().test(((ProfilingEventContext) profilerNotification.getSource()));
  }

  @Override
  public void dispose() {
    if (!notificationManager.isDisposed()) {
      addedListeners.forEach(listener -> notificationManager.removeListener(listener));
    }
  }

  protected abstract ProfilerDataConsumerDiscoveryStrategy getDiscoveryStrategy();

  public <T extends ProfilingEventContext> void notifyEvent(T profilerEventContext, ProfilingEventType<T> action) {
    serverNotificationHandler.fireNotification(new ProfilingNotification(profilerEventContext, action));
  }
}
