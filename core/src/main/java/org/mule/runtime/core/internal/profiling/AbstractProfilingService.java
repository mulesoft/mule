/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_PROFILING_SERVICE;
import static org.mule.runtime.core.internal.profiling.notification.ProfilingNotification.getFullyQualifiedProfilingNotificationIdentifier;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
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

  private final Set<NotificationListener<?>> addedListeners = new HashSet<>();

  @Override
  public void initialise() throws InitialisationException {}

  @Override
  public void start() throws MuleException {
    if (featureFlags.isEnabled(ENABLE_PROFILING_SERVICE)) {
      registerNotificationListeners(getDiscoveryStrategy().discover());
    }
  }

  private void registerNotificationListeners(Set<ProfilingDataConsumer<? extends ProfilingEventContext>> profilingDataConsumers) {
    profilingDataConsumers.forEach(this::registerNotificationListener);
  }

  private <T extends ProfilingEventContext> void registerNotificationListener(ProfilingDataConsumer<T> profilingDataConsumer) {
    NotificationListener<ProfilingNotification<T>> profilingNotificationListener =
        new DefaultProfilingNotificationListener<>(profilingDataConsumer);
    notificationManager
        .addListenerSubscription(profilingNotificationListener,
                                 pn -> filterByAction(profilingDataConsumer, pn));
    addedListeners.add(profilingNotificationListener);
  }

  private <T extends ProfilingEventContext> boolean filterByAction(ProfilingDataConsumer<T> profilingDataConsumer,
                                                                   ProfilingNotification<T> profilingNotification) {
    return profilingDataConsumer.getProfilingEventTypes().stream()
        .anyMatch(
                  eventType -> (getFullyQualifiedProfilingNotificationIdentifier(eventType))
                      .equalsIgnoreCase(profilingNotification.getActionName()))
        &&
        profilingDataConsumer.getEventContextFilter().test((T) profilingNotification.getSource());
  }

  @Override
  public void stop() {
    if (!notificationManager.isDisposed()) {
      addedListeners.forEach(listener -> notificationManager.removeListener(listener));
    }
  }

  /**
   * @return returns the {@link ProfilingDataConsumerDiscoveryStrategy} used by this service.
   */
  protected abstract ProfilingDataConsumerDiscoveryStrategy getDiscoveryStrategy();

  public <T extends ProfilingEventContext> void notifyEvent(T profilingEventContext, ProfilingEventType<T> action) {
    serverNotificationHandler.fireNotification(new ProfilingNotification<>(profilingEventContext, action));
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
