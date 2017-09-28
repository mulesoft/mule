/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.NotificationConfig.EVENT_MAP;
import static org.mule.runtime.config.internal.NotificationConfig.INTERFACE_MAP;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.context.notification.NotificationsProvider;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;

public class ServerNotificationManagerConfigurator extends AbstractComponent implements Initialisable {

  @Inject
  private MuleContext muleContext;
  @Inject
  private Registry registry;
  @Inject
  private ApplicationContext applicationContext;

  private Boolean dynamic;
  private List<NotificationConfig<? extends Notification, ? extends NotificationListener>> enabledNotifications =
      new ArrayList<>();
  private List<NotificationConfig<? extends Notification, ? extends NotificationListener>> disabledNotifications =
      new ArrayList<>();
  private Collection<ListenerSubscriptionPair> notificationListeners;

  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  @Override
  public void initialise() throws InitialisationException {
    Map<String, Class<? extends Notification>> eventMap = new HashMap<>(EVENT_MAP);
    Map<String, Class<? extends NotificationListener>> interfaceMap = new HashMap<>(INTERFACE_MAP);

    ServerNotificationManager notificationManager = populateNotificationTypeMappings(eventMap, interfaceMap);

    enableNotifications(notificationManager, eventMap, interfaceMap);
    disableNotifications(notificationManager, eventMap, interfaceMap);

    // Merge:
    // i) explicitly configured notification listeners,
    // ii) any singleton beans defined in spring that implement NotificationListener.
    for (ListenerSubscriptionPair sub : getMergedListeners(notificationManager)) {
      // Do this to avoid warnings when the Spring context is refreshed
      if (!notificationManager.isListenerRegistered(sub.getListener())) {
        notificationManager.addListenerSubscriptionPair(sub);
      } else {
        notificationManager.removeListener(sub.getListener());
        notificationManager.addListenerSubscriptionPair(sub);
      }
    }
  }

  public ServerNotificationManager populateNotificationTypeMappings(Map<String, Class<? extends Notification>> eventMap,
                                                                    Map<String, Class<? extends NotificationListener>> interfaceMap)
      throws InitialisationException {
    Map<String, NotificationsProvider> providersMap = new HashMap<>();

    for (NotificationsProvider provider : registry.lookupAllByType(NotificationsProvider.class)) {
      for (Entry<String, Pair<Class<? extends Notification>, Class<? extends NotificationListener>>> entry : provider
          .getEventListenerMapping().entrySet()) {

        final String notificationType = entry.getKey();
        if (!notificationType.matches("[a-zA-Z]+:[A-Z\\-]+")) {
          throw new InitialisationException(createStaticMessage("Notification '%s' declared in '%s' doesn't comply with the '[artifactID]:[NOTIFICATION-ID]' format",
                                                                notificationType, provider.toString()),
                                            this);
        }

        if (eventMap.containsKey(notificationType)) {
          throw new InitialisationException(createStaticMessage("Notification '%s' declared in '%s' is already declared for another artifact in provider '%s'.",
                                                                notificationType, provider.toString(),
                                                                eventMap.get(notificationType)),
                                            this);
        }

        eventMap.put(notificationType, entry.getValue().getFirst());
        interfaceMap.put(notificationType, entry.getValue().getSecond());
        providersMap.put(notificationType, provider);
      }
    }

    ServerNotificationManager notificationManager = muleContext.getNotificationManager();
    if (dynamic != null) {
      notificationManager.setNotificationDynamic(dynamic.booleanValue());
    }
    return notificationManager;
  }

  private void disableNotifications(ServerNotificationManager notificationManager,
                                    Map<String, Class<? extends Notification>> eventMap,
                                    Map<String, Class<? extends NotificationListener>> interfaceMap)
      throws InitialisationException {
    for (NotificationConfig<?, ?> disabledNotification : disabledNotifications) {
      final Supplier<InitialisationException> noNotificationExceptionSupplier =
          () -> new InitialisationException(createStaticMessage("No notification '%s' declared in this applications plugins to disable. Expected one of %s",
                                                                disabledNotification.getEventName(),
                                                                eventMap.keySet().toString()),
                                            this);

      if (disabledNotification.isInterfaceExplicitlyConfigured()) {
        notificationManager
            .disableInterface(getInterfaceClass(disabledNotification, interfaceMap).orElseThrow(noNotificationExceptionSupplier));
      }
      if (disabledNotification.isEventExplicitlyConfigured()) {
        notificationManager
            .disableType(getEventClass(disabledNotification, eventMap).orElseThrow(noNotificationExceptionSupplier));
      }
    }
  }

  private void enableNotifications(ServerNotificationManager notificationManager,
                                   Map<String, Class<? extends Notification>> eventMap,
                                   Map<String, Class<? extends NotificationListener>> interfaceMap)
      throws InitialisationException {

    for (NotificationConfig<?, ?> notification : enabledNotifications) {
      final Supplier<InitialisationException> noNotificationExceptionSupplier =
          () -> new InitialisationException(createStaticMessage("No notification '%s' declared in this applications plugins to enable. Expected one of %s",
                                                                notification.getEventName(), eventMap.keySet().toString()),
                                            this);

      notificationManager
          .addInterfaceToType(getInterfaceClass(notification, interfaceMap).orElseThrow(noNotificationExceptionSupplier),
                              getEventClass(notification, eventMap).orElseThrow(noNotificationExceptionSupplier));
    }
  }

  private Optional<Class<? extends Notification>> getEventClass(NotificationConfig config,
                                                                Map<String, Class<? extends Notification>> eventMap) {
    if (config.getEventClass() != null) {
      return of(config.getEventClass());
    }
    if (config.getEventName() != null) {
      return ofNullable(eventMap.get(config.getEventName()));
    }
    return ofNullable(eventMap.get(config.getInterfaceName()));
  }

  private Optional<Class<? extends NotificationListener>> getInterfaceClass(NotificationConfig config,
                                                                            Map<String, Class<? extends NotificationListener>> interfaceMap) {
    if (config.getInterfaceClass() != null) {
      return of(config.getInterfaceClass());
    }
    if (config.getInterfaceName() != null) {
      return of(interfaceMap.get(config.getInterfaceName()));
    }
    return ofNullable(interfaceMap.get(config.getEventName()));
  }

  protected Set<ListenerSubscriptionPair> getMergedListeners(ServerNotificationManager notificationManager) {
    Set<ListenerSubscriptionPair> mergedListeners = new HashSet<>();

    // Any singleton bean defined in spring that implements
    // NotificationListener or a subclass.
    Set<ListenerSubscriptionPair> adhocListeners = new HashSet<>();
    for (String name : applicationContext.getBeanNamesForType(NotificationListener.class, false, true)) {
      adhocListeners.add(new ListenerSubscriptionPair((NotificationListener<?>) applicationContext.getBean(name)));
    }

    if (notificationListeners != null) {
      mergedListeners.addAll(notificationListeners);

      for (ListenerSubscriptionPair candidate : adhocListeners) {
        boolean explicityDefined = false;
        for (ListenerSubscriptionPair explicitListener : notificationListeners) {
          if (candidate.getListener().equals(explicitListener.getListener())) {
            explicityDefined = true;
            break;
          }
        }
        if (!explicityDefined) {
          mergedListeners.add(candidate);
        }
      }
    } else {
      mergedListeners.addAll(adhocListeners);
    }

    return mergedListeners;
  }

  public void setNotificationDynamic(boolean dynamic) {
    this.dynamic = new Boolean(dynamic);
  }

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public void setEnabledNotifications(List<NotificationConfig<? extends Notification, ? extends NotificationListener>> enabledNotifications) {
    this.enabledNotifications = enabledNotifications;
  }

  public void setNotificationListeners(Collection<ListenerSubscriptionPair> notificationListeners) {
    this.notificationListeners = notificationListeners;
  }

  public void setDisabledNotifications(List<NotificationConfig<? extends Notification, ? extends NotificationListener>> disabledNotifications) {
    this.disabledNotifications = disabledNotifications;
  }

  interface DisableNotificationTask {

    void run() throws ClassNotFoundException;
  }
}
