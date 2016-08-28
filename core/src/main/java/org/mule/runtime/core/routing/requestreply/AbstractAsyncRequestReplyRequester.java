/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.requestreply;

import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_SESSION_PROPERTY;
import static org.mule.runtime.core.context.notification.RoutingNotification.MISSED_ASYNC_REPLY;
import static org.mule.runtime.core.message.Correlation.NOT_SET;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.RequestReplyRequesterMessageProcessor;
import org.mule.runtime.core.api.routing.ResponseTimeoutException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.RoutingNotification;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessorBase;
import org.mule.runtime.core.routing.EventProcessingThread;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.core.util.concurrent.ThreadNameHelper;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;

public abstract class AbstractAsyncRequestReplyRequester extends AbstractInterceptingMessageProcessorBase
    implements RequestReplyRequesterMessageProcessor, FlowConstructAware, Initialisable, Startable, Stoppable, Disposable {

  public static final int MAX_PROCESSED_GROUPS = 50000;
  public static final int UNCLAIMED_TIME_TO_LIVE = 60000;
  public static int UNCLAIMED_INTERVAL = 60000;


  public static final String NAME_TEMPLATE = "%s.%s.%s.asyncReplies";
  protected String name;

  protected volatile long timeout = -1;
  protected volatile boolean failOnTimeout = true;
  protected MessageSource replyMessageSource;
  protected FlowConstruct flowConstruct;
  private final MessageProcessor internalAsyncReplyMessageProcessor = new InternalAsyncReplyMessageProcessor();
  private AsyncReplyMonitoringThread replyThread;
  protected final Map<String, Latch> locks = new ConcurrentHashMap<>();
  private String storePrefix = "";

  protected final ConcurrentMap<String, MuleEvent> responseEvents = new ConcurrentHashMap<>();
  protected final Object processedLock = new Object();
  // @GuardedBy processedLock
  protected final BoundedFifoBuffer processed = new BoundedFifoBuffer(MAX_PROCESSED_GROUPS);

  protected ListableObjectStore store;

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    if (replyMessageSource == null) {
      return processNext(event);
    } else {
      locks.put(getAsyncReplyCorrelationId(event), createEventLock());

      sendAsyncRequest(event);

      MuleEvent resultEvent = receiveAsyncReply(event);

      if (resultEvent != null) {
        // If result has MULE_SESSION property then merge session properties returned with existing
        // session properties. See MULE-5852
        if (resultEvent.getMessage().getInboundProperty(MULE_SESSION_PROPERTY) != null) {
          event.getSession().merge(resultEvent.getSession());
        }
        resultEvent = MuleEvent.builder(event).message(resultEvent.getMessage()).build();
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
    name = String.format(NAME_TEMPLATE, storePrefix, ThreadNameHelper.getPrefix(muleContext),
                         flowConstruct == null ? "" : flowConstruct.getName());
    store = ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER))
        .getObjectStore(name, false, MAX_PROCESSED_GROUPS, UNCLAIMED_TIME_TO_LIVE, UNCLAIMED_INTERVAL);
  }

  @Override
  public void start() throws MuleException {
    replyThread = new AsyncReplyMonitoringThread(name);
    replyThread.start();
  }

  @Override
  public void stop() throws MuleException {
    if (replyThread != null) {
      replyThread.stopProcessing();
    }
  }

  @Override
  public void dispose() {
    if (store != null) {
      try {
        ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).disposeStore(store);
      } catch (ObjectStoreException e) {
        logger.debug("Exception disposingg of store", e);
      }
    }
  }

  public void setStorePrefix(String storePrefix) {
    this.storePrefix = storePrefix;
  }

  protected void verifyReplyMessageSource(MessageSource messageSource) {
    // template method
  }

  protected String getAsyncReplyCorrelationId(MuleEvent event) {
    return event.getCorrelationId() + event.getCorrelation().getSequence().map(v -> v.toString()).orElse(NOT_SET);
  }

  protected void sendAsyncRequest(MuleEvent event) throws MuleException {
    processNext(event);
  }

  protected MuleEvent receiveAsyncReply(MuleEvent event) throws MessagingException {
    String asyncReplyCorrelationId = getAsyncReplyCorrelationId(event);
    System.out.println("receiveAsyncReply: " + asyncReplyCorrelationId);
    Latch asyncReplyLatch = locks.get(asyncReplyCorrelationId);
    // flag for catching the interrupted status of the Thread waiting for a
    // result
    boolean interruptedWhileWaiting = false;
    boolean resultAvailable = false;
    MuleEvent result = null;

    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Waiting for async reply message with id: " + asyncReplyCorrelationId);
      }
      // how long should we wait for the lock?
      if (timeout <= 0) {
        asyncReplyLatch.await();
        resultAvailable = true;
      } else {
        resultAvailable = asyncReplyLatch.await(timeout, TimeUnit.MILLISECONDS);
      }
      if (!resultAvailable) {
        postLatchAwait(asyncReplyCorrelationId);
        asyncReplyLatch.await(1000, TimeUnit.MILLISECONDS);
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
        muleContext.fireNotification(new RoutingNotification(event.getMessage(), null, RoutingNotification.ASYNC_REPLY_TIMEOUT));

        throw new ResponseTimeoutException(CoreMessages.responseTimedOutWaitingForId((int) timeout,
                                                                                     asyncReplyCorrelationId),
                                           event, null);
      } else {
        return null;
      }
    }
  }

  protected void postLatchAwait(String asyncReplyCorrelationId) throws MessagingException {
    // Template method
  }

  protected void addProcessed(Object id) {
    synchronized (processedLock) {
      if (processed.isFull()) {
        processed.remove();
      }
      processed.add(id);
    }
  }

  protected boolean isAlreadyProcessed(Object id) {
    synchronized (processedLock) {
      return processed.contains(id);
    }
  }

  class InternalAsyncReplyMessageProcessor implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      store.store(getAsyncReplyCorrelationId(event), event);
      replyThread.processNow();
      return null;
    }
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  private class AsyncReplyMonitoringThread extends EventProcessingThread {

    AsyncReplyMonitoringThread(String name) {
      super(name, 100);
    }

    @Override
    protected void doRun() {
      try {
        List<Serializable> ids = store.allKeys();
        logger.debug("Found " + ids.size() + " objects in store");
        for (Serializable id : ids) {
          try {
            boolean deleteEvent = false;
            String correlationId = (String) id;

            if (isAlreadyProcessed(correlationId)) {
              deleteEvent = true;
              MuleEvent event = (MuleEvent) store.retrieve(correlationId);
              if (logger.isDebugEnabled()) {
                logger.debug("An event was received for an event group that has already been processed, "
                    + "this is probably because the async-reply timed out. Correlation Id is: "
                    + correlationId + ". Dropping event");
              }
              // Fire a notification to say we received this message
              muleContext
                  .fireNotification(new RoutingNotification(event.getMessage(), event.getContext().getOriginatingConnectorName(),
                                                            MISSED_ASYNC_REPLY));
            } else {
              Latch l = locks.get(correlationId);
              if (l != null) {
                MuleEvent event = retrieveEvent(correlationId);

                MuleEvent previousResult = responseEvents.putIfAbsent(correlationId, event);
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

    private MuleEvent retrieveEvent(String correlationId) throws ObjectStoreException, DefaultMuleException {
      MuleEvent event = (MuleEvent) store.retrieve(correlationId);

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
}
