/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.notificationListenerSubscriptionAlreadyRegistered;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.propertyIsNotSupportedType;
import static org.mule.runtime.core.api.context.notification.ServerNotificationManager.toClass;

import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;

import java.util.Collection;
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
public class Configuration {

  protected static Logger logger = LoggerFactory.getLogger(Configuration.class);
  // map from interface to collection of events
  private Map<Class<? extends NotificationListener>, Set<Class<? extends Notification>>> interfaceToTypes =
      new HashMap<>();
  private Set<ListenerSubscriptionPair> listenerSubscriptionPairs = new HashSet<>();
  private Set<Class<? extends NotificationListener>> disabledInterfaces = new HashSet<>();
  private Set<Class<? extends Notification>> disabledNotificationTypes = new HashSet<>();
  private volatile boolean dirty = true;
  private Policy policy;

  public synchronized void addInterfaceToType(Class<? extends NotificationListener> iface,
                                              Class<? extends Notification> type) {
    dirty = true;
    if (!Notification.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException(propertyIsNotSupportedType("type", Notification.class, type)
          .getMessage());
    }
    if (!interfaceToTypes.containsKey(iface)) {
      interfaceToTypes.put(iface, new HashSet<Class<? extends Notification>>());
    }
    Set<Class<? extends Notification>> events = interfaceToTypes.get(iface);
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
  public synchronized void addAllInterfaceToTypes(Map<Class<? extends NotificationListener>, Set<Class<? extends Notification>>> interfaceToTypes)
      throws ClassNotFoundException {
    dirty = true;

    for (Object iface : interfaceToTypes.keySet()) {
      addInterfaceToType(toClass(iface), toClass(interfaceToTypes.get(iface)));
    }
  }

  public synchronized void addListenerSubscriptionPair(ListenerSubscriptionPair pair) {
    dirty = true;
    if (!listenerSubscriptionPairs.add(pair)) {
      logger.warn(notificationListenerSubscriptionAlreadyRegistered(pair).toString());
    }
  }

  public synchronized void removeListener(NotificationListener listener) {
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
      removeListener((NotificationListener) listener.next());
    }
  }

  public synchronized void disableInterface(Class<? extends NotificationListener> iface) {
    dirty = true;
    disabledInterfaces.add(iface);
  }

  public synchronized void disabledAllInterfaces(Collection<Class<? extends NotificationListener>> interfaces)
      throws ClassNotFoundException {
    dirty = true;
    for (Object element : interfaces) {
      disableInterface(toClass(element));
    }
  }

  public synchronized void disableType(Class<? extends Notification> type) {
    dirty = true;
    disabledNotificationTypes.add(type);
  }

  synchronized void disableAllTypes(Collection types) throws ClassNotFoundException {
    dirty = true;
    for (Iterator event = types.iterator(); event.hasNext();) {
      disableType(toClass(event.next()));
    }
  }

  public Policy getPolicy() {
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

  public Map<Class<? extends NotificationListener>, Set<Class<? extends Notification>>> getInterfaceToTypes() {
    return unmodifiableMap(interfaceToTypes);
  }

  public Set<ListenerSubscriptionPair> getListeners() {
    return unmodifiableSet(listenerSubscriptionPairs);
  }

}
