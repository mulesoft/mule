/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.correlation;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.notification.RoutingNotification.CORRELATION_TIMEOUT;
import static org.mule.runtime.api.notification.RoutingNotification.MISSED_AGGREGATION_GROUP_EVENT;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.correlationTimedOut;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.message.GroupCorrelation.NOT_SET;
import static org.mule.runtime.core.api.util.StringMessageUtils.truncate;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.RoutingNotification;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.PartitionableObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.StringMessageUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.routing.EventGroup;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.routing.RoutingException;
import org.mule.runtime.core.privileged.store.DeserializationPostInitialisable;
import org.mule.runtime.core.privileged.util.monitor.Expirable;
import org.mule.runtime.core.privileged.util.monitor.ExpiryMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class EventCorrelator implements Startable, Stoppable {

  /**
   * logger used by this class
   */
  protected transient final Logger logger = LoggerFactory.getLogger(EventCorrelator.class);

  public static final String NO_CORRELATION_ID = "no-id";

  private static final long DELAY_TIME = 10;

  protected final Object groupsLock = new Object();

  // @GuardedBy groupsLock
  protected ObjectStore<Long> processedGroups = null;

  private long timeout = -1; // undefined

  private boolean failOnTimeout = true;

  private MuleContext muleContext;

  private NotificationDispatcher notificationFirer;

  private EventCorrelatorCallback callback;

  private Processor timeoutMessageProcessor;

  /**
   * A map of EventGroup objects in a partition. These represent one or more messages to be agregated, keyed by message id. There
   * will be one response message for every EventGroup.
   */
  private PartitionableObjectStore correlatorStore = null;
  private String storePrefix;

  private Scheduler scheduler;
  private ExpiringGroupMonitoringRunnable expiringGroupRunnable;
  private final String name;

  private final FlowConstruct flowConstruct;

  public EventCorrelator(EventCorrelatorCallback callback, Processor timeoutMessageProcessor, MuleContext muleContext,
                         FlowConstruct flowConstruct, PartitionableObjectStore correlatorStore, String storePrefix,
                         ObjectStore<Long> processedGroups) {
    if (callback == null) {
      throw new IllegalArgumentException(objectIsNull("EventCorrelatorCallback").getMessage());
    }
    if (muleContext == null) {
      throw new IllegalArgumentException(objectIsNull("MuleContext").getMessage());
    }
    this.callback = callback;
    this.muleContext = muleContext;
    try {
      this.notificationFirer = ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationDispatcher.class);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
    this.timeoutMessageProcessor = timeoutMessageProcessor;
    name = format("%s.event.correlator", flowConstruct.getName());
    this.flowConstruct = flowConstruct;

    this.correlatorStore = correlatorStore;
    this.storePrefix = storePrefix;
    this.processedGroups = processedGroups;
  }

  public void forceGroupExpiry(String groupId) throws MuleException {
    try {
      if (correlatorStore.retrieve(groupId, getEventGroupsPartitionKey()) != null) {
        handleGroupExpiry(getEventGroup(groupId));
      } else {
        addProcessedGroup(groupId);
      }
    } catch (ObjectStoreException e) {
      // TODO improve this
      throw new MessagingException(null, e);
    }
  }

  public CoreEvent process(CoreEvent event) throws RoutingException {
    // the correlationId of the event's message
    final String groupId = event.getCorrelationId();

    if (logger.isTraceEnabled()) {
      try {
        logger.trace(format("Received async reply message for correlationID: %s%n%s%n%s", groupId,
                            truncate(StringMessageUtils.toString(event.getMessage().getPayload().getValue()), 200, false),
                            event.getMessage().toString()));
      } catch (Exception e) {
        // ignore
      }
    }

    // spinloop for the EventGroup lookup
    while (true) {
      try {
        if (isGroupAlreadyProcessed(groupId)) {
          if (logger.isDebugEnabled()) {
            logger.debug("An event was received for an event group that has already been processed, "
                + "this is probably because the async-reply timed out. GroupCorrelation Id is: " + groupId + ". Dropping event");
          }
          // Fire a notification to say we received this message
          notificationFirer.dispatch(new RoutingNotification(event.getMessage(), event.getContext().getOriginatingLocation()
              .getComponentIdentifier().getIdentifier().getNamespace(),
                                                             MISSED_AGGREGATION_GROUP_EVENT));
          return null;
        }
      } catch (ObjectStoreException e) {
        throw new RoutingException(timeoutMessageProcessor, e);
      }

      // check for an existing group first
      EventGroup group;
      try {
        group = this.getEventGroup(groupId);
      } catch (ObjectStoreException e) {
        throw new RoutingException(timeoutMessageProcessor, e);
      }

      // does the group exist?
      if (group == null) {
        // ..apparently not, so create a new one & add it
        try {
          EventGroup eventGroup = callback.createEventGroup(event, groupId);
          eventGroup.initEventsStore(correlatorStore);
          group = this.addEventGroup(eventGroup);
        } catch (ObjectStoreException e) {
          throw new RoutingException(timeoutMessageProcessor, e);
        }
      }

      // ensure that only one thread at a time evaluates this EventGroup
      synchronized (groupsLock) {
        if (logger.isDebugEnabled()) {
          logger.debug("Adding event to aggregator group: " + groupId);
        }

        // add the incoming event to the group
        try {
          group.addEvent(event);
        } catch (ObjectStoreException e) {
          throw new RoutingException(timeoutMessageProcessor, e);
        }

        // check to see if the event group is ready to be aggregated
        if (callback.shouldAggregateEvents(group)) {
          // create the response event
          CoreEvent returnEvent = null;
          try {
            returnEvent = callback.aggregateEvents(group);
          } catch (RoutingException routingException) {
            try {
              this.removeEventGroup(group);
              group.clear();
            } catch (ObjectStoreException objectStoreException) {
              throw new RoutingException(timeoutMessageProcessor, objectStoreException);
            }
            throw routingException;
          }
          // remove the eventGroup as no further message will be received
          // for this group once we aggregate
          try {
            this.removeEventGroup(group);
            group.clear();
          } catch (ObjectStoreException e) {
            throw new RoutingException(timeoutMessageProcessor, e);
          }

          return returnEvent;
        } else {
          return null;
        }
      }
    }
  }

  protected EventGroup getEventGroup(String groupId) throws ObjectStoreException {
    try {
      EventGroup eventGroup = (EventGroup) correlatorStore.retrieve(groupId, getEventGroupsPartitionKey());
      if (!eventGroup.isInitialised()) {
        try {
          DeserializationPostInitialisable.Implementation.init(eventGroup, muleContext);
        } catch (Exception e) {
          throw new ObjectStoreException(e);
        }
      }
      eventGroup.initEventsStore(correlatorStore);
      return eventGroup;
    } catch (ObjectDoesNotExistException e) {
      return null;
    }
  }

  protected EventGroup addEventGroup(EventGroup group) throws ObjectStoreException {
    try {
      correlatorStore.store((String) group.getGroupId(), group, getEventGroupsPartitionKey());
      return group;
    } catch (ObjectAlreadyExistsException e) {
      return getEventGroup((String) group.getGroupId());
    }
  }

  protected void removeEventGroup(EventGroup group) throws ObjectStoreException {
    final Object groupId = group.getGroupId();
    synchronized (groupsLock) {
      if (!isGroupAlreadyProcessed(groupId)) {
        correlatorStore.remove((String) groupId, getEventGroupsPartitionKey());
        addProcessedGroup(groupId);
      }
    }
  }

  protected void addProcessedGroup(Object id) throws ObjectStoreException {
    synchronized (groupsLock) {
      processedGroups.store((String) id, currentTimeMillis());
    }
  }

  protected boolean isGroupAlreadyProcessed(Object id) throws ObjectStoreException {
    synchronized (groupsLock) {
      return processedGroups.contains((String) id);
    }
  }

  public boolean isFailOnTimeout() {
    return failOnTimeout;
  }

  public void setFailOnTimeout(boolean failOnTimeout) {
    this.failOnTimeout = failOnTimeout;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  protected void handleGroupExpiry(EventGroup group) throws MuleException {
    try {
      removeEventGroup(group);
    } catch (ObjectStoreException e) {
      throw new DefaultMuleException(e);
    }

    if (isFailOnTimeout()) {
      CoreEvent messageCollectionEvent = group.getMessageCollectionEvent();
      notificationFirer.dispatch(new RoutingNotification(messageCollectionEvent.getMessage(), null, CORRELATION_TIMEOUT));
      try {
        group.clear();
      } catch (ObjectStoreException e) {
        logger.warn("Failed to clear group with id " + group.getGroupId() + " since underlying ObjectStore threw Exception:"
            + e.getMessage());
      }
      throw new CorrelationTimeoutException(correlationTimedOut(group.getGroupId()));
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(MessageFormat.format(
                                          "Aggregator expired, but ''failOnTimeOut'' is false. Forwarding {0} events out of {1} "
                                              + "total for group ID: {2}",
                                          group.size(), group.expectedSize().map(v -> v.toString()).orElse(NOT_SET),
                                          group.getGroupId()));
      }

      try {
        if (!(group.getCreated() + DAYS.toMillis(1) < currentTimeMillis())) {
          CoreEvent newEvent = CoreEvent.builder(callback.aggregateEvents(group)).build();
          group.clear();

          if (!correlatorStore.contains((String) group.getGroupId(), getExpiredAndDispatchedPartitionKey())) {
            // TODO which use cases would need a sync reply event
            // returned?
            if (timeoutMessageProcessor != null) {
              timeoutMessageProcessor.process(newEvent);
            } else {
              throw new MessagingException(createStaticMessage(MessageFormat
                  .format("Group {0} timed out, but no timeout message processor was " + "configured.", group.getGroupId())),
                                           newEvent);
            }
            correlatorStore.store((String) group.getGroupId(), group.getCreated(), getExpiredAndDispatchedPartitionKey());
          } else {
            logger.warn(MessageFormat.format("Discarding group {0}", group.getGroupId()));
          }
        }
      } catch (MessagingException me) {
        throw me;
      } catch (Exception e) {
        throw new MessagingException(group.getMessageCollectionEvent(), e);
      }
    }
  }

  @Override
  public void start() throws MuleException {
    logger.info("Starting event correlator: " + name);
    if (timeout != 0) {
      scheduler = muleContext.getSchedulerService().customScheduler(muleContext.getSchedulerBaseConfig().withName(name)
          .withMaxConcurrentTasks(1).withShutdownTimeout(0, MILLISECONDS));
      expiringGroupRunnable = new ExpiringGroupMonitoringRunnable();
      scheduler.scheduleWithFixedDelay(expiringGroupRunnable, 0, DELAY_TIME, MILLISECONDS);
    }
  }

  @Override
  public void stop() throws MuleException {
    logger.info("Stopping event correlator: " + name);
    if (scheduler != null) {
      scheduler.stop();
    }
    if (expiringGroupRunnable != null) {
      expiringGroupRunnable.dispose();
      expiringGroupRunnable = null;
    }
  }

  private final class ExpiringGroupMonitoringRunnable implements Runnable, Expirable, Disposable {

    private ExpiryMonitor expiryMonitor;

    public ExpiringGroupMonitoringRunnable() {
      this.expiryMonitor = new ExpiryMonitor(name, MINUTES.toMillis(1), muleContext, true);
      this.expiryMonitor.addExpirable(30, MINUTES, this);
    }

    /**
     * Removes the elements in expiredAndDispatchedGroups when groupLife is reached
     */
    @Override
    public void expired() {
      try {
        for (String o : (List<String>) correlatorStore.allKeys(getExpiredAndDispatchedPartitionKey())) {
          Long time = (Long) correlatorStore.retrieve(o, getExpiredAndDispatchedPartitionKey());
          if (time + DAYS.toMillis(1) < currentTimeMillis()) {
            correlatorStore.remove(o, getExpiredAndDispatchedPartitionKey());
            logger.warn(MessageFormat.format("Discarding group {0}", o));
          }
        }
      } catch (ObjectStoreException e) {
        logger.warn("Expiration of objects failed due to ObjectStoreException " + e + ".");
      }
    }

    @Override
    public void run() {

      //// TODO(pablo.kraan): is not good to have threads doing nothing in all the nodes but the primary. Need to
      //// start the thread on the primary node only, and then use a notification schema to start a new thread
      //// in a different node when the primary goes down.
      if (!muleContext.isPrimaryPollingInstance()) {
        return;
      }

      List<EventGroup> expired = new ArrayList<>(1);
      try {
        for (String o : (List<String>) correlatorStore.allKeys(getEventGroupsPartitionKey())) {
          EventGroup group = getEventGroup(o);
          // group may have been removed by another thread right after eventGroups.allKeys()
          if (group != null && group.getCreated() + getTimeout() < currentTimeMillis()) {
            expired.add(group);
          }
        }
      } catch (ObjectStoreException e) {
        logger.warn("expiry failed dues to ObjectStoreException " + e);
      }
      for (final EventGroup group : expired) {
        try {
          handleGroupExpiry(group);
        } catch (MessagingException e) {
          flowConstruct.getExceptionListener().handleException(e, e.getEvent());
        } catch (Exception e) {
          muleContext.getExceptionListener().handleException(e);
        }
      }
    }

    @Override
    public void dispose() {
      if (expiryMonitor != null) {
        expiryMonitor.dispose();
      }
    }
  }

  protected String getExpiredAndDispatchedPartitionKey() {
    return storePrefix + ".expiredAndDispatchedGroups";
  }

  protected String getEventGroupsPartitionKey() {
    return storePrefix + ".eventGroups";
  }
}
