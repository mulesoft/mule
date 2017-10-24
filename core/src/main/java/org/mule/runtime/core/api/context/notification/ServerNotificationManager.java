/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.AsyncMessageNotification;
import org.mule.runtime.api.notification.AsyncMessageNotificationListener;
import org.mule.runtime.api.notification.ClusterNodeNotification;
import org.mule.runtime.api.notification.ClusterNodeNotificationListener;
import org.mule.runtime.api.notification.ConnectionNotification;
import org.mule.runtime.api.notification.ConnectionNotificationListener;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.notification.ConnectorMessageNotificationListener;
import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.api.notification.ErrorHandlerNotificationListener;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.FlowConstructNotification;
import org.mule.runtime.api.notification.FlowConstructNotificationListener;
import org.mule.runtime.api.notification.ManagementNotification;
import org.mule.runtime.api.notification.ManagementNotificationListener;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.mule.runtime.api.notification.RoutingNotification;
import org.mule.runtime.api.notification.RoutingNotificationListener;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.notification.SecurityNotificationListener;
import org.mule.runtime.api.notification.TransactionNotification;
import org.mule.runtime.api.notification.TransactionNotificationListener;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.internal.context.notification.Configuration;
import org.mule.runtime.core.internal.context.notification.Policy;
import org.mule.runtime.core.privileged.context.notification.OptimisedNotificationHandler;

import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

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
 * {@link OptimisedNotificationHandler} was added. This allows a service that generates notifications to cache locally a handler
 * optimised for a particular class.
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
public class ServerNotificationManager implements ServerNotificationHandler, MuleContextAware {

  private static final Logger logger = getLogger(ServerNotificationManager.class);

  private boolean dynamic = false;
  private Configuration configuration = new Configuration();
  private AtomicInteger activeFires = new AtomicInteger();
  private AtomicBoolean disposed = new AtomicBoolean(false);
  private Latch disposeLatch = new Latch();
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

  /**
   * Do not make this object {@link org.mule.runtime.api.lifecycle.Initialisable}. It needs to be initialised before every other
   * object to send notifications.
   */
  public void initialise() throws InitialisationException {
    notificationsLiteScheduler = muleContext.getSchedulerService().cpuLightScheduler();
    notificationsIoScheduler = muleContext.getSchedulerService().ioScheduler();
  }

  public void addInterfaceToType(Class<? extends NotificationListener> iface,
                                 Class<? extends Notification> event) {
    configuration.addInterfaceToType(iface, event);
  }

  public void setInterfaceToTypes(Map<Class<? extends NotificationListener>, Set<Class<? extends Notification>>> interfaceToEvents)
      throws ClassNotFoundException {
    configuration.addAllInterfaceToTypes(interfaceToEvents);
  }

  public void addListenerSubscriptionPair(ListenerSubscriptionPair pair) {
    configuration.addListenerSubscriptionPair(pair);
  }

  public void addListener(NotificationListener<?> listener) {
    configuration.addListenerSubscriptionPair(new ListenerSubscriptionPair(listener));
  }

  public <N extends Notification> void addListenerSubscription(NotificationListener<N> listener,
                                                               Predicate<N> selector) {
    configuration.addListenerSubscriptionPair(new ListenerSubscriptionPair(listener, selector));
  }

  /**
   * This removes *all* registrations that reference this listener
   */
  public void removeListener(NotificationListener<?> listener) {
    configuration.removeListener(listener);
  }

  public void disableInterface(Class<? extends NotificationListener> iface) {
    configuration.disableInterface(iface);
  }

  public void setDisabledInterfaces(Collection<Class<? extends NotificationListener>> interfaces)
      throws ClassNotFoundException {
    configuration.disabledAllInterfaces(interfaces);
  }

  public void disableType(Class<? extends Notification> type) {
    configuration.disableType(type);
  }

  @Override
  public boolean isListenerRegistered(NotificationListener listener) {
    for (ListenerSubscriptionPair pair : configuration.getListeners()) {
      if (pair.getListener().equals(listener)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void fireNotification(Notification notification) {
    if (disposed.get()) {
      logger.warn("Notification not enqueued after ServerNotificationManager disposal: " + notification);
      return;
    }

    activeFires.incrementAndGet();
    try {
      if (notification instanceof AbstractServerNotification) {
        ((AbstractServerNotification) notification).setServerId(muleContext.getId());
      }
      if (notification.isSynchronous()) {
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
      if (0 == activeFires.decrementAndGet() && disposed.get()) {
        disposeLatch.countDown();
      }
    }
  }

  protected void notifyListeners(Notification notification, NotifierCallback notifier) {
    configuration.getPolicy().dispatch(notification, notifier);
  }

  @Override
  public boolean isNotificationEnabled(Class<? extends Notification> type) {
    boolean enabled = false;
    if (configuration != null) {
      Policy policy = configuration.getPolicy();
      if (policy != null) {
        enabled = policy.isNotificationEnabled(type);
      }
    }
    return enabled;
  }

  /**
   * Do not make this object {@link org.mule.runtime.api.lifecycle.Disposable}. It needs to be alive after everything else has
   * died
   */
  public void dispose() {
    disposed.set(true);

    if (activeFires.get() > 0) {
      try {
        disposeLatch.await();
      } catch (InterruptedException e) {
        // Continue with the disposal after interrupt
        currentThread().interrupt();
      }
    }

    if (notificationsLiteScheduler != null) {
      notificationsLiteScheduler.stop();
      notificationsLiteScheduler = null;
    }
    if (notificationsIoScheduler != null) {
      notificationsIoScheduler.stop();
      notificationsIoScheduler = null;
    }

    configuration = null;
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

  public Map<Class<? extends NotificationListener>, Set<Class<? extends Notification>>> getInterfaceToTypes() {
    return unmodifiableMap(configuration.getInterfaceToTypes());
  }

  public Set<ListenerSubscriptionPair> getListeners() {
    return unmodifiableSet(configuration.getListeners());
  }

  public boolean isDisposed() {
    return disposed.get();
  }

  /**
   * @return a {@link ServerNotificationManager} with the default configuration for Mule notifications
   */
  public static ServerNotificationManager createDefaultNotificationManager() {
    ServerNotificationManager manager = new ServerNotificationManager();
    manager.addInterfaceToType(MuleContextNotificationListener.class, MuleContextNotification.class);
    manager.addInterfaceToType(RoutingNotificationListener.class, RoutingNotification.class);
    manager.addInterfaceToType(SecurityNotificationListener.class, SecurityNotification.class);
    manager.addInterfaceToType(ManagementNotificationListener.class, ManagementNotification.class);
    manager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);
    manager.addInterfaceToType(ConnectionNotificationListener.class, ConnectionNotification.class);
    manager.addInterfaceToType(ExceptionNotificationListener.class, ExceptionNotification.class);
    manager.addInterfaceToType(ErrorHandlerNotificationListener.class, ErrorHandlerNotification.class);
    manager.addInterfaceToType(TransactionNotificationListener.class, TransactionNotification.class);
    manager.addInterfaceToType(PipelineMessageNotificationListener.class, PipelineMessageNotification.class);
    manager.addInterfaceToType(AsyncMessageNotificationListener.class, AsyncMessageNotification.class);
    manager.addInterfaceToType(ClusterNodeNotificationListener.class, ClusterNodeNotification.class);
    manager.addInterfaceToType(ConnectorMessageNotificationListener.class, ConnectorMessageNotification.class);
    manager.addInterfaceToType(ErrorHandlerNotificationListener.class, ErrorHandlerNotification.class);
    manager.addInterfaceToType(FlowConstructNotificationListener.class, FlowConstructNotification.class);

    return manager;
  }
}
