/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.notification.BlockingServerEvent;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.ClassUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * {@link org.mule.runtime.core.context.notification.OptimisedNotificationHandler} was added. This allows a service that generates
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
public class ServerNotificationManager
    implements Initialisable, Disposable, Startable, Stoppable, ServerNotificationHandler, MuleContextAware {

  private static final Logger logger = getLogger(ServerNotificationManager.class);

  private boolean dynamic = false;
  private Configuration configuration = new Configuration();
  private AtomicBoolean disposed = new AtomicBoolean(false);
  // private volatile Thread runningThread;
  // private BlockingDeque<ServerNotification> eventQueue = new LinkedBlockingDeque<>();
  private MuleContext muleContext;
  private Scheduler notificationsLiteScheduler;
  private Scheduler notificationsIoScheduler;
  private Scheduler notificationsComputationScheduler;

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
    // try {
    // notificationsScheduler = muleContext.getRegistry().lookupObject(SchedulerService.class).cpuLightScheduler();
    // } catch (RegistrationException e) {
    // throw new InitialisationException(e, this);
    // }
  }

  @Override
  public void start() throws InitialisationException {
    try {
      notificationsLiteScheduler = muleContext.getRegistry().lookupObject(SchedulerService.class).cpuLightScheduler();
      notificationsIoScheduler = muleContext.getRegistry().lookupObject(SchedulerService.class).ioScheduler();
      notificationsComputationScheduler = muleContext.getRegistry().lookupObject(SchedulerService.class).computationScheduler();
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }
  }

  // public void start(WorkManager workManager, WorkListener workListener) throws LifecycleException {
  // try {
  // workManager.scheduleWork(this, WorkManager.INDEFINITE, null, workListener);
  // } catch (WorkException e) {
  // throw new LifecycleException(e, this);
  // }
  // }

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
    if (!disposed.get()) {
      notification.setMuleContext(muleContext);
      if (notification instanceof BlockingServerEvent) {
        notifyListeners(notification, (listener, nfn) -> listener.onNotification(nfn));
      } else {
        notifyListeners(notification, (listener, nfn) -> {
          if (CPU_LITE.equals(listener.getProcessingType())) {
            notificationsLiteScheduler.submit(() -> listener.onNotification(nfn));
          } else if (BLOCKING.equals(listener.getProcessingType())) {
            notificationsIoScheduler.submit(() -> listener.onNotification(nfn));
          } else if (CPU.equals(listener.getProcessingType())) {
            notificationsComputationScheduler.submit(() -> listener.onNotification(nfn));
          }
        });
        // notificationsScheduler.submit(() -> notifyListeners(notification));
        // try {
        // eventQueue.put(notification);
        // } catch (InterruptedException e) {
        // if (!disposed.get()) {
        // logger.error("Failed to queue notification: " + notification, e);
        // }
        // }
      }
    } else {
      logger.warn("Notification not enqueued after ServerNotificationManager disposal: " + notification);
    }
  }

  @FunctionalInterface
  public interface Notifier {

    void notify(ServerNotificationListener listener, ServerNotification notification);
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
  public void stop() {
    if (notificationsLiteScheduler != null) {
      notificationsLiteScheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
      notificationsLiteScheduler = null;
    }
    if (notificationsIoScheduler != null) {
      notificationsIoScheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
      notificationsIoScheduler = null;
    }
    if (notificationsComputationScheduler != null) {
      notificationsComputationScheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
      notificationsComputationScheduler = null;
    }
  }

  @Override
  public void dispose() {
    disposed.set(true);
    configuration = null;
    // if (runningThread != null) {
    // runningThread.interrupt();
    // }
  }

  protected void notifyListeners(ServerNotification notification, Notifier notifier) {
    if (!disposed.get()) {
      configuration.getPolicy().dispatch(notification, notifier);
    } else {
      logger.warn("Notification not delivered after ServerNotificationManager disposal: " + notification);
    }
  }

  // @Override
  // public void release() {
  // dispose();
  // }
  //
  // @Override
  // public void run() {
  // runningThread = currentThread();
  // while (!disposed.get()) {
  // try {
  // int timeout = muleContext.getConfiguration().getDefaultQueueTimeout();
  // ServerNotification notification = eventQueue.poll(timeout, TimeUnit.MILLISECONDS);
  // if (notification != null) {
  // notifyListeners(notification);
  // }
  // } catch (InterruptedException e) {
  // currentThread().interrupt();
  // }
  // }
  // }

  /**
   * Support string or class parameters
   */
  static Class toClass(Object value) throws ClassNotFoundException {
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

}
