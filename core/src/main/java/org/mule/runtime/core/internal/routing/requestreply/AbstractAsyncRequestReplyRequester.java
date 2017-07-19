/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.requestreply;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_SESSION_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.responseTimedOutWaitingForId;
import static org.mule.runtime.core.api.context.notification.RoutingNotification.ASYNC_REPLY_TIMEOUT;
import static org.mule.runtime.core.api.context.notification.RoutingNotification.MISSED_ASYNC_REPLY;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.notification.RoutingNotification;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.RequestReplyRequesterMessageProcessor;
import org.mule.runtime.core.api.routing.ResponseTimeoutException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.store.DeserializationPostInitialisable;
import org.mule.runtime.core.api.util.ObjectUtils;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.processor.AbstractInterceptingMessageProcessorBase;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;

public abstract class AbstractAsyncRequestReplyRequester extends AbstractInterceptingMessageProcessorBase
    implements RequestReplyRequesterMessageProcessor, Initialisable, Startable, Stoppable, Disposable {

  private static final int MAX_PROCESSED_GROUPS = 50000;
  private static final long UNCLAIMED_TIME_TO_LIVE = 60000;
  private static final long UNCLAIMED_INTERVAL = 60000;
  private static final String NAME_TEMPLATE = "%s.%s.%s.asyncReplies";

  protected String name;

  protected volatile long timeout = -1;
  protected volatile boolean failOnTimeout = true;
  protected MessageSource replyMessageSource;
  private final Processor internalAsyncReplyMessageProcessor = new InternalAsyncReplyMessageProcessor();
  private Scheduler scheduler;
  private AsyncReplyMonitoringRunnable replyRunnable;
  protected final Map<String, Latch> locks = new ConcurrentHashMap<>();
  private String storePrefix = "";

  protected final ConcurrentMap<String, Event> responseEvents = new ConcurrentHashMap<>();
  private final Object processedLock = new Object();
  // @GuardedBy processedLock
  private final BoundedFifoBuffer processed = new BoundedFifoBuffer(MAX_PROCESSED_GROUPS);

  protected ObjectStore store;

  @Override
  public Event process(Event event) throws MuleException {
    if (replyMessageSource == null) {
      return processNext(event);
    } else {
      locks.put(getAsyncReplyCorrelationId(event), createEventLock());

      sendAsyncRequest(event);

      Event resultEvent = receiveAsyncReply(event);

      if (resultEvent != null) {
        // If result has MULE_SESSION property then merge session properties returned with existing
        // session properties. See MULE-5852
        if (((InternalMessage) resultEvent.getMessage()).getInboundProperty(MULE_SESSION_PROPERTY) != null) {
          event.getSession().merge(resultEvent.getSession());
        }
        resultEvent = Event.builder(event).message(resultEvent.getMessage()).build();
        setCurrentEvent(resultEvent);
      }
      return resultEvent;
    }
  }

  /**
   * Creates the lock used to synchronize a given event
   *
   * @return a new Latch instance
   */
  protected Latch createEventLock() {
    return new Latch();
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public void setFailOnTimeout(boolean failOnTimeout) {
    this.failOnTimeout = failOnTimeout;
  }

  @Override
  public void setReplySource(MessageSource messageSource) {
    verifyReplyMessageSource(messageSource);
    replyMessageSource = messageSource;
    messageSource.setListener(internalAsyncReplyMessageProcessor);
  }

  @Override
  public void initialise() throws InitialisationException {
    name = format(NAME_TEMPLATE, storePrefix, muleContext.getConfiguration().getId(), getLocation().getRootContainerName());
    store = ((ObjectStoreManager) muleContext.getRegistry().get(OBJECT_STORE_MANAGER))
        .createObjectStore(name, ObjectStoreSettings.builder()
            .persistent(false)
            .maxEntries(MAX_PROCESSED_GROUPS)
            .entryTtl(UNCLAIMED_TIME_TO_LIVE)
            .expirationInterval(UNCLAIMED_INTERVAL)
            .build());
  }

  @Override
  public void start() throws MuleException {
    scheduler = muleContext.getSchedulerService().customScheduler(muleContext.getSchedulerBaseConfig().withName(name)
        .withMaxConcurrentTasks(1)
        .withShutdownTimeout(0, MILLISECONDS));
    replyRunnable = new AsyncReplyMonitoringRunnable();
    scheduler.scheduleWithFixedDelay(replyRunnable, 0, 100, MILLISECONDS);
  }

  @Override
  public void stop() throws MuleException {
    scheduler.stop();
  }

  @Override
  public void dispose() {
    if (store != null) {
      try {
        ((ObjectStoreManager) muleContext.getRegistry().get(OBJECT_STORE_MANAGER)).disposeStore(name);
      } catch (ObjectStoreException e) {
        logger.debug("Exception disposing of store", e);
      }
    }
  }

  public void setStorePrefix(String storePrefix) {
    this.storePrefix = storePrefix;
  }

  protected void verifyReplyMessageSource(MessageSource messageSource) {
    // template method
  }

  private String getAsyncReplyCorrelationId(Event event) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(event.getContext().getCorrelationId());
    event.getGroupCorrelation().getSequence().ifPresent(v -> stringBuilder.append("-" + v));
    return stringBuilder.toString();
  }

  protected void sendAsyncRequest(Event event) throws MuleException {
    processNext(event);
  }

  private Event receiveAsyncReply(Event event) throws MuleException {
    String asyncReplyCorrelationId = getAsyncReplyCorrelationId(event);
    System.out.println("receiveAsyncReply: " + asyncReplyCorrelationId);
    Latch asyncReplyLatch = locks.get(asyncReplyCorrelationId);
    // flag for catching the interrupted status of the Thread waiting for a
    // result
    boolean interruptedWhileWaiting = false;
    boolean resultAvailable = false;
    Event result;

    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Waiting for async reply message with id: " + asyncReplyCorrelationId);
      }
      // how long should we wait for the lock?
      if (timeout <= 0) {
        asyncReplyLatch.await();
        resultAvailable = true;
      } else {
        resultAvailable = asyncReplyLatch.await(timeout, MILLISECONDS);
      }
      if (!resultAvailable) {
        postLatchAwait(asyncReplyCorrelationId);
        asyncReplyLatch.await(1000, MILLISECONDS);
        resultAvailable = asyncReplyLatch.getCount() == 0;
      }
    } catch (InterruptedException e) {
      interruptedWhileWaiting = true;
    } finally {
      locks.remove(asyncReplyCorrelationId);
      result = responseEvents.remove(asyncReplyCorrelationId);
      if (interruptedWhileWaiting) {
        Thread.currentThread().interrupt();
        return null;
      }
    }

    if (resultAvailable) {
      if (result == null) {
        // this should never happen, just using it as a safe guard for now
        throw new IllegalStateException("Response MuleEvent is null");
      }
      // Copy event because the async-reply message was received by a different
      // receiver thread (or the senders dispatcher thread in case of vm
      // with queueEvents="false") and the current thread may need to mutate
      // the even. See MULE-4370
      setCurrentEvent(result);
      return result;
    } else {
      addProcessed(asyncReplyCorrelationId);

      if (failOnTimeout) {
        muleContext.fireNotification(new RoutingNotification(event.getMessage(), null, ASYNC_REPLY_TIMEOUT));

        throw new ResponseTimeoutException(responseTimedOutWaitingForId((int) timeout, asyncReplyCorrelationId), null);
      } else {
        return null;
      }
    }
  }

  private void postLatchAwait(String asyncReplyCorrelationId) throws MessagingException {
    // Template method
  }

  private void addProcessed(Object id) {
    synchronized (processedLock) {
      if (processed.isFull()) {
        processed.remove();
      }
      processed.add(id);
    }
  }

  private boolean isAlreadyProcessed(Object id) {
    synchronized (processedLock) {
      return processed.contains(id);
    }
  }

  class InternalAsyncReplyMessageProcessor extends AbstractAnnotatedObject implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      store.store(getAsyncReplyCorrelationId(event), event);
      replyRunnable.run();
      return null;
    }
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

  private class AsyncReplyMonitoringRunnable implements Runnable {

    @Override
    public void run() {
      try {
        List<Serializable> ids = store.allKeys();
        logger.debug("Found " + ids.size() + " objects in store");
        for (Serializable id : ids) {
          try {
            boolean deleteEvent = false;
            String correlationId = (String) id;

            if (isAlreadyProcessed(correlationId)) {
              deleteEvent = true;
              Event event = (Event) store.retrieve(correlationId);
              if (logger.isDebugEnabled()) {
                logger.debug("An event was received for an event group that has already been processed, "
                    + "this is probably because the async-reply timed out. GroupCorrelation Id is: "
                    + correlationId + ". Dropping event");
              }
              // Fire a notification to say we received this message
              muleContext
                  .fireNotification(new RoutingNotification(event.getMessage(), event.getContext().getOriginatingLocation()
                      .getComponentIdentifier().getIdentifier().getNamespace(),
                                                            MISSED_ASYNC_REPLY));
            } else {
              Latch l = locks.get(correlationId);
              if (l != null) {
                Event event = retrieveEvent(correlationId);

                Event previousResult = responseEvents.putIfAbsent(correlationId, event);
                if (previousResult != null) {
                  // this would indicate that we need a better way to prevent
                  // continued aggregation for a group that is currently being
                  // processed. Can this actually happen?
                  throw new IllegalStateException("Detected duplicate result message with id: " + correlationId);
                }
                addProcessed(correlationId);
                deleteEvent = true;
                l.countDown();
              }
            }
            if (deleteEvent) {
              store.remove(correlationId);
            }
          } catch (Exception ex) {
            logger.debug("Error processing async replies", ex);
          }
        }
      } catch (Exception ex) {
        logger.debug("Error processing async replies", ex);
      }
    }

    private Event retrieveEvent(String correlationId) throws ObjectStoreException, DefaultMuleException {
      Event event = (Event) store.retrieve(correlationId);

      // TODO MULE-10302 remove this.
      if (event.getFlowConstruct() == null) {
        try {
          DeserializationPostInitialisable.Implementation.init(event, muleContext);
        } catch (Exception e) {
          throw new DefaultMuleException(e);
        }
      }

      return event;
    }
  }

  @Override
  public ProcessingType getProcessingType() {
    return BLOCKING;
  }
}
