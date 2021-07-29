/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.ProfilerDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.internal.profiling.notification.ProfilingNotification;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_PROFILING_SERVICE;

/**
 * A {@link AbstractProfilingService} that discovers available {@link ProfilingDataConsumer}
 *
 * @since 4.4
 */
public abstract class AbstractProfilingService implements ProfilingService, Initialisable, Startable, Stoppable {

  @Inject
  protected ServerNotificationManager notificationManager;

  @Inject
  private ServerNotificationHandler serverNotificationHandler;

  private FeatureFlaggingService featureFlags;

  private final Set<NotificationListener<ProfilingNotification>> addedListeners = new HashSet<>();

  @Override
  public void initialise() throws InitialisationException {}

  @Override
  public void start() throws MuleException {
    if (featureFlags.isEnabled(ENABLE_PROFILING_SERVICE)) {
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
    return profilerDataConsumer.getProfilingEventTypes().stream()
        .anyMatch(
                  eventType -> eventType.getProfilingEventTypeIdentifier()
                      .equals(profilerNotification.getActionName()))
        &&
        profilerDataConsumer.getEventContextFilter().test(((ProfilingEventContext) profilerNotification.getSource()));
  }

  @Override
  public void stop() {
    if (!notificationManager.isDisposed()) {
      addedListeners.forEach(listener -> notificationManager.removeListener(listener));
    }
  }

  /**
   * @return returns the {@link ProfilerDataConsumerDiscoveryStrategy} used by this service.
   */
  protected abstract ProfilerDataConsumerDiscoveryStrategy getDiscoveryStrategy();

  public <T extends ProfilingEventContext> void notifyEvent(T profilerEventContext, ProfilingEventType<T> action) {
    serverNotificationHandler.fireNotification(new ProfilingNotification(profilerEventContext, action));
  }

  @Inject
  public void setFeatureFlags(FeatureFlaggingService featureFlags) {
    this.featureFlags = featureFlags;
  }

  /**
   * Configures {@link FeatureFlaggingService} for the profiles functionality
   *
   * @since 4.4
   */
  public static void configureEnableProfilingService() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(ENABLE_PROFILING_SERVICE,
                                                featureContext -> featureContext.getArtifactMinMuleVersion()
                                                    .filter(muleVersion -> muleVersion
                                                        .atLeast(ENABLE_PROFILING_SERVICE.getSince()))
                                                    .isPresent());
  }
}
