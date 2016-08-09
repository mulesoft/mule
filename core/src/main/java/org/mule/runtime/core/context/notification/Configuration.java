/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import static org.mule.runtime.core.context.notification.ServerNotificationManager.toClass;

import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This acts as a synchronized collection. No call blocks and all are synchronized.
 */
class Configuration {

  protected static Logger logger = LoggerFactory.getLogger(Configuration.class);
  // map from interface to collection of events
  private Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> interfaceToTypes =
      new HashMap<>();
  private Set<ListenerSubscriptionPair> listenerSubscriptionPairs = new HashSet<>();
  private Set<Class<? extends ServerNotificationListener>> disabledInterfaces =
      new HashSet<>();
  private Set<Class<? extends ServerNotification>> disabledNotificationTypes = new HashSet<>();
  private volatile boolean dirty = true;
  private Policy policy;

  synchronized void addInterfaceToType(Class<? extends ServerNotificationListener> iface,
                                       Class<? extends ServerNotification> type) {
    dirty = true;
    if (!ServerNotification.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException(CoreMessages.propertyIsNotSupportedType("type", ServerNotification.class, type)
          .getMessage());
    }
    if (!interfaceToTypes.containsKey(iface)) {
      interfaceToTypes.put(iface, new HashSet<Class<? extends ServerNotification>>());
    }
    Set<Class<? extends ServerNotification>> events = interfaceToTypes.get(iface);
    events.add(type);
    if (logger.isDebugEnabled()) {
      logger.debug("Registered event type: " + type);
      logger.debug("Binding listener type '" + iface + "' to event type '" + type + "'");
    }
  }

  /**
   * @param interfaceToTypes map from interace to a particular event
   * @throws ClassNotFoundException if the interface is a key, but the corresponding class cannot be loaded
   */
  synchronized void addAllInterfaceToTypes(Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> interfaceToTypes)
      throws ClassNotFoundException {
    dirty = true;

    for (Object iface : interfaceToTypes.keySet()) {
      addInterfaceToType(toClass(iface), toClass(interfaceToTypes.get(iface)));
    }
  }

  synchronized void addListenerSubscriptionPair(ListenerSubscriptionPair pair) {
    dirty = true;
    if (!listenerSubscriptionPairs.add(pair)) {
      logger.warn(CoreMessages.notificationListenerSubscriptionAlreadyRegistered(pair).toString());
    }
  }

  synchronized void addAllListenerSubscriptionPairs(Collection pairs) {
    dirty = true;
    for (Iterator listener = pairs.iterator(); listener.hasNext();) {
      addListenerSubscriptionPair((ListenerSubscriptionPair) listener.next());
    }
  }

  synchronized void removeListener(ServerNotificationListener listener) {
    dirty = true;
    Set<ListenerSubscriptionPair> toRemove = new HashSet<>();
    for (Object element : listenerSubscriptionPairs) {
      ListenerSubscriptionPair pair = (ListenerSubscriptionPair) element;
      if (pair.getListener().equals(listener)) {
        toRemove.add(pair);
      }
    }
    listenerSubscriptionPairs.removeAll(toRemove);
  }

  synchronized void removeAllListeners(Collection listeners) {
    dirty = true;
    for (Iterator listener = listeners.iterator(); listener.hasNext();) {
      removeListener((ServerNotificationListener) listener.next());
    }
  }

  synchronized void disableInterface(Class<? extends ServerNotificationListener> iface) {
    dirty = true;
    disabledInterfaces.add(iface);
  }

  synchronized void disabledAllInterfaces(Collection<Class<? extends ServerNotificationListener>> interfaces)
      throws ClassNotFoundException {
    dirty = true;
    for (Object element : interfaces) {
      disableInterface(toClass(element));
    }
  }

  synchronized void disableType(Class<? extends ServerNotification> type) {
    dirty = true;
    disabledNotificationTypes.add(type);
  }

  synchronized void disableAllTypes(Collection types) throws ClassNotFoundException {
    dirty = true;
    for (Iterator event = types.iterator(); event.hasNext();) {
      disableType(toClass(event.next()));
    }
  }

  protected Policy getPolicy() {
    if (dirty) {
      synchronized (this) {
        if (dirty) {
          policy = new Policy(interfaceToTypes, listenerSubscriptionPairs, disabledInterfaces, disabledNotificationTypes);
          dirty = false;
        }
      }
    }
    return policy;
  }

  // for tests -------------------------------

  Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> getInterfaceToTypes() {
    return Collections.unmodifiableMap(interfaceToTypes);
  }

  Set<ListenerSubscriptionPair> getListeners() {
    return Collections.unmodifiableSet(listenerSubscriptionPairs);
  }

}
