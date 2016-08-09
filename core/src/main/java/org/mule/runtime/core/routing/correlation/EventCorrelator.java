/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.correlation;

import static org.mule.runtime.core.message.Correlation.NOT_SET;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.core.api.store.ObjectDoesNotExistException;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.PartitionableObjectStore;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.RoutingNotification;
import org.mule.runtime.core.execution.ErrorHandlingExecutionTemplate;
import org.mule.runtime.core.routing.EventGroup;
import org.mule.runtime.core.routing.EventProcessingThread;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.concurrent.ThreadNameHelper;
import org.mule.runtime.core.util.monitor.Expirable;
import org.mule.runtime.core.util.monitor.ExpiryMonitor;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventCorrelator implements Startable, Stoppable, Disposable {

  /**
   * logger used by this class
   */
  protected transient final Logger logger = LoggerFactory.getLogger(EventCorrelator.class);

  public static final String NO_CORRELATION_ID = "no-id";

  private static final long ONE_DAY_IN_MILLI = 1000 * 60 * 60 * 24;

  protected long groupTimeToLive = ONE_DAY_IN_MILLI;

  protected final Object groupsLock = new Object();

  // @GuardedBy groupsLock
  protected ObjectStore<Long> processedGroups = null;

  private long timeout = -1; // undefined

  private boolean failOnTimeout = true;

  private MuleContext muleContext;

  private EventCorrelatorCallback callback;

  private MessageProcessor timeoutMessageProcessor;

  /**
   * A map of EventGroup objects in a partition. These represent one or more messages to be agregated, keyed by message id. There
   * will be one response message for every EventGroup.
   */
  private PartitionableObjectStore correlatorStore = null;
  private String storePrefix;

  private EventCorrelator.ExpiringGroupMonitoringThread expiringGroupMonitoringThread;
  private final String name;

  private final FlowConstruct flowConstruct;

  public EventCorrelator(EventCorrelatorCallback callback, MessageProcessor timeoutMessageProcessor, MuleContext muleContext,
                         FlowConstruct flowConstruct, PartitionableObjectStore correlatorStore, String storePrefix,
                         ObjectStore<Long> processedGroups) {
    if (callback == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("EventCorrelatorCallback").getMessage());
    }
    if (muleContext == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("MuleContext").getMessage());
    }
    this.callback = callback;
    this.muleContext = muleContext;
    this.timeoutMessageProcessor = timeoutMessageProcessor;
    name = String.format("%s%s.event.correlator", ThreadNameHelper.getPrefix(muleContext), flowConstruct.getName());
    this.flowConstruct = flowConstruct;

    this.correlatorStore = correlatorStore;
    this.storePrefix = storePrefix;
    this.processedGroups = processedGroups;
  }

  public void forceGroupExpiry(String groupId) throws MessagingException {
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

  public MuleEvent process(MuleEvent event) throws RoutingException {
    // the correlationId of the event's message
    final String groupId = event.getMessage().getCorrelation().getId().orElse(event.getMessage().getUniqueId());

    if (logger.isTraceEnabled()) {
      try {
        logger.trace(String.format("Received async reply message for correlationID: %s%n%s%n%s",
                                   groupId, StringMessageUtils
                                       .truncate(StringMessageUtils.toString(event.getMessage().getPayload()), 200, false),
                                   StringMessageUtils.headersToString(event.getMessage())));
      } catch (Exception e) {
        // ignore
      }
    }
    if (groupId == null || groupId.equals("-1")) {
      throw new RoutingException(CoreMessages.noCorrelationId(), event, timeoutMessageProcessor);
    }

    // spinloop for the EventGroup lookup
    while (true) {
      try {
        if (isGroupAlreadyProcessed(groupId)) {
          if (logger.isDebugEnabled()) {
            logger.debug("An event was received for an event group that has already been processed, "
                + "this is probably because the async-reply timed out. Correlation Id is: " + groupId + ". Dropping event");
          }
          // Fire a notification to say we received this message
          muleContext.fireNotification(new RoutingNotification(event.getMessage(), event.getMessageSourceURI().toString(),
                                                               RoutingNotification.MISSED_AGGREGATION_GROUP_EVENT));
          return null;
        }
      } catch (ObjectStoreException e) {
        throw new RoutingException(event, timeoutMessageProcessor, e);
      }

      // check for an existing group first
      EventGroup group;
      try {
        group = this.getEventGroup(groupId);
      } catch (ObjectStoreException e) {
        throw new RoutingException(event, timeoutMessageProcessor, e);
      }

      // does the group exist?
      if (group == null) {
        // ..apparently not, so create a new one & add it
        try {
          EventGroup eventGroup = callback.createEventGroup(event, groupId);
          eventGroup.initEventsStore(correlatorStore);
          group = this.addEventGroup(eventGroup);
        } catch (ObjectStoreException e) {
          throw new RoutingException(event, timeoutMessageProcessor, e);
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
          throw new RoutingException(event, timeoutMessageProcessor, e);
        }

        // check to see if the event group is ready to be aggregated
        if (callback.shouldAggregateEvents(group)) {
          // create the response event
          MuleEvent returnEvent = callback.aggregateEvents(group);
          final Builder builder = MuleMessage.builder(returnEvent.getMessage()).correlationId(groupId);
          String rootId = group.getCommonRootId();
          if (rootId != null) {
            builder.rootId(rootId);
          }

          returnEvent.setMessage(builder.build());

          // remove the eventGroup as no further message will be received
          // for this group once we aggregate
          try {
            this.removeEventGroup(group);
            group.clear();
          } catch (ObjectStoreException e) {
            throw new RoutingException(event, timeoutMessageProcessor, e);
          }

          return returnEvent;
        } else {
          return null;
        }
      }
    }
  }

  protected EventGroup getEventGroup(Serializable groupId) throws ObjectStoreException {
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
      correlatorStore.store((Serializable) group.getGroupId(), group, getEventGroupsPartitionKey());
      return group;
    } catch (ObjectAlreadyExistsException e) {
      return getEventGroup((String) group.getGroupId());
    }
  }

  protected void removeEventGroup(EventGroup group) throws ObjectStoreException {
    final Object groupId = group.getGroupId();
    synchronized (groupsLock) {
      if (!isGroupAlreadyProcessed(groupId)) {
        correlatorStore.remove((Serializable) groupId, getEventGroupsPartitionKey());
        addProcessedGroup(groupId);
      }
    }
  }

  protected void addProcessedGroup(Object id) throws ObjectStoreException {
    synchronized (groupsLock) {
      processedGroups.store((Serializable) id, System.currentTimeMillis());
    }
  }

  protected boolean isGroupAlreadyProcessed(Object id) throws ObjectStoreException {
    synchronized (groupsLock) {
      return processedGroups.contains((Serializable) id);
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

  protected void handleGroupExpiry(EventGroup group) throws MessagingException {
    try {
      removeEventGroup(group);
    } catch (ObjectStoreException e) {
      throw new MessagingException(group.getMessageCollectionEvent(), e);
    }

    if (isFailOnTimeout()) {
      MuleEvent messageCollectionEvent = group.getMessageCollectionEvent();
      muleContext.fireNotification(new RoutingNotification(messageCollectionEvent.getMessage(), null,
                                                           RoutingNotification.CORRELATION_TIMEOUT));
      try {
        group.clear();
      } catch (ObjectStoreException e) {
        logger.warn("Failed to clear group with id " + group.getGroupId() + " since underlying ObjectStore threw Exception:"
            + e.getMessage());
      }
      throw new CorrelationTimeoutException(CoreMessages.correlationTimedOut(group.getGroupId()), messageCollectionEvent);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(MessageFormat.format(
                                          "Aggregator expired, but ''failOnTimeOut'' is false. Forwarding {0} events out of {1} "
                                              + "total for group ID: {2}",
                                          group.size(), group.expectedSize().map(v -> v.toString()).orElse(NOT_SET),
                                          group.getGroupId()));
      }

      try {
        if (!(group.getCreated() + groupTimeToLive < System.currentTimeMillis())) {
          MuleEvent newEvent = callback.aggregateEvents(group);
          group.clear();
          newEvent.setMessage(MuleMessage.builder(newEvent.getMessage()).correlationId(group.getGroupId().toString()).build());

          if (!correlatorStore.contains((Serializable) group.getGroupId(), getExpiredAndDispatchedPartitionKey())) {
            // TODO which use cases would need a sync reply event
            // returned?
            if (timeoutMessageProcessor != null) {
              timeoutMessageProcessor.process(newEvent);
            } else {
              throw new MessagingException(CoreMessages.createStaticMessage(MessageFormat
                  .format("Group {0} timed out, but no timeout message processor was " + "configured.", group.getGroupId())),
                                           newEvent);
            }
            correlatorStore.store((Serializable) group.getGroupId(), group.getCreated(), getExpiredAndDispatchedPartitionKey());
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
      expiringGroupMonitoringThread = new ExpiringGroupMonitoringThread();
      expiringGroupMonitoringThread.start();
    }
  }

  @Override
  public void stop() throws MuleException {
    logger.info("Stopping event correlator: " + name);
    if (expiringGroupMonitoringThread != null) {
      expiringGroupMonitoringThread.stopProcessing();
    }
  }

  private final class ExpiringGroupMonitoringThread extends EventProcessingThread implements Expirable, Disposable {

    private ExpiryMonitor expiryMonitor;
    public static final long DELAY_TIME = 10;

    public ExpiringGroupMonitoringThread() {
      super(name, DELAY_TIME);
      this.expiryMonitor = new ExpiryMonitor(name, 1000 * 60, muleContext, true);
      // clean up every 30 minutes
      this.expiryMonitor.addExpirable(1000 * 60 * 30, TimeUnit.MILLISECONDS, this);
    }

    /**
     * Removes the elements in expiredAndDispatchedGroups when groupLife is reached
     *
     * @throws ObjectStoreException
     */
    @Override
    public void expired() {
      try {
        for (Serializable o : (List<Serializable>) correlatorStore.allKeys(getExpiredAndDispatchedPartitionKey())) {
          Long time = (Long) correlatorStore.retrieve(o, getExpiredAndDispatchedPartitionKey());
          if (time + groupTimeToLive < System.currentTimeMillis()) {
            correlatorStore.remove(o, getExpiredAndDispatchedPartitionKey());
            logger.warn(MessageFormat.format("Discarding group {0}", o));
          }
        }
      } catch (ObjectStoreException e) {
        logger.warn("Expiration of objects failed due to ObjectStoreException " + e + ".");
      }
    }

    @Override
    public void doRun() {

      //// TODO(pablo.kraan): is not good to have threads doing nothing in all the nodes but the primary. Need to
      //// start the thread on the primary node only, and then use a notification schema to start a new thread
      //// in a different node when the primary goes down.
      if (!muleContext.isPrimaryPollingInstance()) {
        return;
      }

      List<EventGroup> expired = new ArrayList<>(1);
      try {
        for (Serializable o : (List<Serializable>) correlatorStore.allKeys(getEventGroupsPartitionKey())) {
          EventGroup group = getEventGroup(o);
          // group may have been removed by another thread right after eventGroups.allKeys()
          if (group != null && group.getCreated() + getTimeout() < System.currentTimeMillis()) {
            expired.add(group);
          }
        }
      } catch (ObjectStoreException e) {
        logger.warn("expiry failed dues to ObjectStoreException " + e);
      }
      for (final EventGroup group : expired) {
        ExecutionTemplate<MuleEvent> executionTemplate = ErrorHandlingExecutionTemplate
            .createErrorHandlingExecutionTemplate(muleContext, flowConstruct.getExceptionListener());
        try {
          executionTemplate.execute(() -> {
            handleGroupExpiry(group);
            return null;
          });
        } catch (MessagingException e) {
          // Already handled by TransactionTemplate
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

  @Override
  public void dispose() {
    disposeIfDisposable(expiringGroupMonitoringThread);
  }

  private void disposeIfDisposable(Object o) {
    if (o != null && o instanceof Disposable) {
      ((Disposable) o).dispose();
    }
  }
}
