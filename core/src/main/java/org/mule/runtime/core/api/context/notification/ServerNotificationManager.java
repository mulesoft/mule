/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.internal.context.notification.Configuration;
import org.mule.runtime.core.internal.context.notification.OptimisedNotificationHandler;
import org.mule.runtime.core.internal.context.notification.Policy;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

/**
 * A reworking of the event manager that allows efficient behaviour without global on/off switches in the config.
 *
 * <p>
 * The configuration and resulting policy are separate; the policy is a summary of the configuration that contains information to
 * decide whether a particular message can be handled, and which updates that with experience gained handling messages. When the
 * configuration is changed the policy is rebuilt. In this way we get a fairly efficient system without needing controls
 * elsewhere.
 *
 * <p>
 * However, measurements showed that there was still a small impact on speed in some cases. To improve behaviour further the
 * {@link OptimisedNotificationHandler} was added. This allows a service that generates
 * notifications to cache locally a handler optimised for a particular class.
 *
 * <p>
 * The dynamic flag stops this caching from occurring. This reduces efficiency slightly (about 15% cost on simple VM messages,
 * less on other transports)
 * </p>
 *
 * <p>
 * Note that, because of subclass relationships, we need to be very careful about exactly what is enabled and disabled:
 * <ul>
 * <li>Disabling an event or interface disables all uses of that class or any subclass.</li>
 * <li>Enquiring whether an event is enabled returns true if any subclass is enabled.</li>
 * </ul>
 */
public class ServerNotificationManager implements Initialisable, Disposable, ServerNotificationHandler, MuleContextAware {

  private static final Logger logger = getLogger(ServerNotificationManager.class);

  private boolean dynamic = false;
  private Configuration configuration = new Configuration();
  private ReentrantReadWriteLock disposeLock = new ReentrantReadWriteLock();
  private AtomicBoolean disposed = new AtomicBoolean(false);
  private MuleContext muleContext;
  private Scheduler notificationsLiteScheduler;
  private Scheduler notificationsIoScheduler;

  @Override
  public boolean isNotificationDynamic() {
    return dynamic;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
  }

  public void setNotificationDynamic(boolean dynamic) {
    this.dynamic = dynamic;
  }

  @Override
  public void initialise() throws InitialisationException {
    notificationsLiteScheduler = muleContext.getSchedulerService().cpuLightScheduler();
    notificationsIoScheduler = muleContext.getSchedulerService().ioScheduler();
  }

  public void addInterfaceToType(Class<? extends ServerNotificationListener> iface, Class<? extends ServerNotification> event) {
    configuration.addInterfaceToType(iface, event);
  }

  public void setInterfaceToTypes(Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> interfaceToEvents)
      throws ClassNotFoundException {
    configuration.addAllInterfaceToTypes(interfaceToEvents);
  }

  public void addListenerSubscriptionPair(ListenerSubscriptionPair pair) {
    configuration.addListenerSubscriptionPair(pair);
  }

  public void addListener(ServerNotificationListener<?> listener) {
    configuration.addListenerSubscriptionPair(new ListenerSubscriptionPair(listener));
  }

  public void addListenerSubscription(ServerNotificationListener<?> listener, String subscription) {
    configuration.addListenerSubscriptionPair(new ListenerSubscriptionPair(listener, subscription));
  }

  /**
   * This removes *all* registrations that reference this listener
   */
  public void removeListener(ServerNotificationListener<?> listener) {
    configuration.removeListener(listener);
  }

  public void disableInterface(Class<? extends ServerNotificationListener> iface) throws ClassNotFoundException {
    configuration.disableInterface(iface);
  }

  public void setDisabledInterfaces(Collection<Class<? extends ServerNotificationListener>> interfaces)
      throws ClassNotFoundException {
    configuration.disabledAllInterfaces(interfaces);
  }

  public void disableType(Class<? extends ServerNotification> type) throws ClassNotFoundException {
    configuration.disableType(type);
  }

  @Override
  public boolean isListenerRegistered(ServerNotificationListener listener) {
    for (ListenerSubscriptionPair pair : configuration.getListeners()) {
      if (pair.getListener().equals(listener)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void fireNotification(ServerNotification notification) {
    disposeLock.readLock().lock();
    try {
      if (disposed.get()) {
        logger.warn("Notification not enqueued after ServerNotificationManager disposal: " + notification);
        return;
      }

      notification.setMuleContext(muleContext);
      if (notification instanceof SynchronousServerEvent) {
        notifyListeners(notification, (listener, nfn) -> listener.onNotification(nfn));
      } else {
        notifyListeners(notification, (listener, nfn) -> {
          if (listener.isBlocking()) {
            notificationsIoScheduler.submit(() -> listener.onNotification(nfn));
          } else {
            notificationsLiteScheduler.submit(() -> listener.onNotification(nfn));
          }
        });
      }
    } finally {
      disposeLock.readLock().unlock();
    }
  }

  protected void notifyListeners(ServerNotification notification, NotifierCallback notifier) {
    configuration.getPolicy().dispatch(notification, notifier);
  }

  @Override
  public boolean isNotificationEnabled(Class<? extends ServerNotification> type) {
    boolean enabled = false;
    if (configuration != null) {
      Policy policy = configuration.getPolicy();
      if (policy != null) {
        enabled = policy.isNotificationEnabled(type);
      }
    }
    return enabled;
  }

  @Override
  public void dispose() {
    disposeLock.writeLock().lock();
    try {
      if (notificationsLiteScheduler != null) {
        notificationsLiteScheduler.stop();
        notificationsLiteScheduler = null;
      }
      if (notificationsIoScheduler != null) {
        notificationsIoScheduler.stop();
        notificationsIoScheduler = null;
      }

      disposed.set(true);
      configuration = null;
    } finally {
      disposeLock.writeLock().unlock();
    }
  }

  /**
   * Support string or class parameters
   */
  public static Class toClass(Object value) throws ClassNotFoundException {
    Class clazz;
    if (value instanceof String) {
      clazz = ClassUtils.loadClass(value.toString(), value.getClass());
    } else if (value instanceof Class) {
      clazz = (Class) value;
    } else {
      throw new IllegalArgumentException("Notification types and listeners must be a Class with fully qualified class name. Value is: "
          + value);
    }
    return clazz;
  }

  // for tests -------------------------------------------------------

  Policy getPolicy() {
    return configuration.getPolicy();
  }

  public Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> getInterfaceToTypes() {
    return unmodifiableMap(configuration.getInterfaceToTypes());
  }

  public Set<ListenerSubscriptionPair> getListeners() {
    return unmodifiableSet(configuration.getListeners());
  }

  public boolean isDisposed() {
    return disposed.get();
  }
}
