/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.api.Event.getVariableValueOrNull;
import static org.mule.runtime.core.routing.UntilSuccessful.DEFAULT_PROCESS_ATTEMPT_COUNT_PROPERTY_VALUE;
import static org.mule.runtime.core.routing.UntilSuccessful.PROCESS_ATTEMPT_COUNT_PROPERTY_NAME;
import static org.mule.runtime.core.util.StringUtils.DASH;
import static org.mule.runtime.core.util.store.QueuePersistenceObjectStore.DEFAULT_QUEUE_STORE;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.util.concurrent.ThreadNameHelper;
import org.mule.runtime.core.util.queue.objectstore.QueueKey;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Until successful asynchronous processing strategy.
 * <p/>
 * It will return successfully to the flow executing the router once it was able to store the message in the object store.
 * <p/>
 * After that it will asynchronously try to process the message through the internal route. If route was not successfully executed
 * after the configured retry count then the message will be routed to the defined dead letter queue route or in case there is no
 * dead letter queue route then it will be handled by the flow exception strategy.
 */
public class AsynchronousUntilSuccessfulProcessingStrategy extends AbstractUntilSuccessfulProcessingStrategy
    implements Initialisable, Startable, Stoppable, MessagingExceptionHandlerAware {

  private static final String UNTIL_SUCCESSFUL_MSG_PREFIX = "until-successful retries exhausted. Last exception message was: %s";
  protected transient Logger logger = LoggerFactory.getLogger(getClass());
  private MessagingExceptionHandler messagingExceptionHandler;
  private ExecutorService pool;
  private ScheduledExecutorService scheduledRetriesPool;

  @Override
  public void initialise() throws InitialisationException {
    if (getUntilSuccessfulConfiguration().getObjectStore() == null) {
      throw new InitialisationException(
                                        I18nMessageFactory
                                            .createStaticMessage("A ListableObjectStore must be configured on UntilSuccessful."),
                                        this);
    }
  }

  @Override
  public void start() {
    final String threadPrefix =
        String.format("%s%s.%s", ThreadNameHelper.getPrefix(getUntilSuccessfulConfiguration().getMuleContext()),
                      getUntilSuccessfulConfiguration().getFlowConstruct().getName(), "until-successful");
    pool = getUntilSuccessfulConfiguration().getThreadingProfile().createPool(threadPrefix);
    scheduledRetriesPool = getUntilSuccessfulConfiguration().createScheduledRetriesPool(threadPrefix);

    scheduleAllPendingEventsForProcessing();
  }

  @Override
  public void stop() {
    scheduledRetriesPool.shutdown();
    scheduledRetriesPool = null;
    pool.shutdown();
    pool = null;
  }

  @Override
  protected Event doRoute(Event event, FlowConstruct flow) throws MuleException {
    try {
      final Serializable eventStoreKey = storeEvent(event, flow);
      scheduleForProcessing(eventStoreKey, true);
      if (getUntilSuccessfulConfiguration().getAckExpression() == null) {
        return event;
      }
      return processResponseThroughAckResponseExpression(event);
    } catch (final Exception e) {
      throw new MessagingException(
                                   I18nMessageFactory.createStaticMessage("Failed to schedule the event for processing"), event,
                                   e,
                                   getUntilSuccessfulConfiguration().getRouter());
    }
  }

  private void scheduleAllPendingEventsForProcessing() {
    try {
      for (final Serializable eventStoreKey : getUntilSuccessfulConfiguration().getObjectStore().allKeys()) {
        try {
          scheduleForProcessing(eventStoreKey, true);
        } catch (final Exception e) {
          logger.error(I18nMessageFactory
              .createStaticMessage("Failed to schedule for processing event stored with key: " + eventStoreKey).toString(), e);
        }
      }
    } catch (Exception e) {
      logger.warn("Failure during scheduling of until successful previous jobs " + e.getMessage());
      if (logger.isDebugEnabled()) {
        logger.debug("Failure during scheduling of until successful previous jobs ", e);
      }
    }
  }

  private void scheduleForProcessing(final Serializable eventStoreKey, boolean firstTime) {
    if (firstTime) {
      submitForProcessing(eventStoreKey);
    } else {
      this.scheduledRetriesPool.schedule(() -> {
        submitForProcessing(eventStoreKey);
        return null;
      }, getUntilSuccessfulConfiguration().getMillisBetweenRetries(), TimeUnit.MILLISECONDS);
    }
  }

  protected void submitForProcessing(final Serializable eventStoreKey) {
    this.pool.execute(() -> {
      try {
        retrieveAndProcessEvent(eventStoreKey);
      } catch (Exception e) {
        incrementProcessAttemptCountAndRescheduleOrRemoveFromStore(eventStoreKey, e);
      }
    });
  }

  private void incrementProcessAttemptCountAndRescheduleOrRemoveFromStore(final Serializable eventStoreKey,
                                                                          Exception lastException) {
    try {
      final Event event = getUntilSuccessfulConfiguration().getObjectStore().remove(eventStoreKey);

      final Integer configuredAttempts = getVariableValueOrNull(PROCESS_ATTEMPT_COUNT_PROPERTY_NAME, event);
      final Integer deliveryAttemptCount =
          configuredAttempts != null ? configuredAttempts : DEFAULT_PROCESS_ATTEMPT_COUNT_PROPERTY_VALUE;

      Event incrementedEvent = event;
      if (deliveryAttemptCount <= getUntilSuccessfulConfiguration().getMaxRetries()) {
        // we store the incremented version unless the max attempt count has been reached
        incrementedEvent = Event.builder(incrementedEvent)
            .addVariable(PROCESS_ATTEMPT_COUNT_PROPERTY_NAME, deliveryAttemptCount + 1).build();
        getUntilSuccessfulConfiguration().getObjectStore().store(eventStoreKey, incrementedEvent);
        this.scheduleForProcessing(eventStoreKey, false);
      } else {
        abandonRetries(event, incrementedEvent, lastException);
      }
    } catch (final ObjectStoreException ose) {
      logger.error("Failed to increment failure count for event stored with key: " + eventStoreKey, ose);
    }
  }

  private Serializable storeEvent(final Event event, FlowConstruct flow) throws ObjectStoreException {
    Integer configuredAttempts = getVariableValueOrNull(PROCESS_ATTEMPT_COUNT_PROPERTY_NAME, event);
    final Integer deliveryAttemptCount =
        configuredAttempts != null ? configuredAttempts : DEFAULT_PROCESS_ATTEMPT_COUNT_PROPERTY_VALUE;
    return storeEvent(event, flow, deliveryAttemptCount);
  }

  private Serializable storeEvent(Event event, FlowConstruct flow, final int deliveryAttemptCount)
      throws ObjectStoreException {
    event = Event.builder(event).addVariable(PROCESS_ATTEMPT_COUNT_PROPERTY_NAME, deliveryAttemptCount).build();
    final Serializable eventStoreKey = buildQueueKey(event, flow, muleContext);
    getUntilSuccessfulConfiguration().getObjectStore().store(eventStoreKey, event);
    return eventStoreKey;
  }

  public static Serializable buildQueueKey(final Event muleEvent, FlowConstruct flow, MuleContext muleContext) {
    // the key is built in way to prevent UntilSuccessful workers across a cluster to compete for the same events over a shared
    // object store it also adds a random trailer to support events which have been split and thus have the same id. Random number
    // was chosen over UUID for performance reasons
    StringBuilder keyBuilder = new StringBuilder();
    muleEvent.getGroupCorrelation().getSequence().ifPresent(v -> keyBuilder.append(v + DASH));
    keyBuilder.append(muleEvent.getContext().getId());
    keyBuilder.append(DASH);
    keyBuilder.append(muleContext.getClusterId());
    keyBuilder.append(DASH);
    keyBuilder.append(flow);
    return new QueueKey(DEFAULT_QUEUE_STORE, keyBuilder.toString());
  }

  private void abandonRetries(final Event event, final Event mutableEvent, final Exception lastException) {
    if (getUntilSuccessfulConfiguration().getDlqMP() == null) {
      logger.info("Retry attempts exhausted and no DLQ defined");
      // mutableEvent should be a local copy of event
      messagingExceptionHandler
          .handleException(new MessagingException(mutableEvent, buildRetryPolicyExhaustedException(lastException)), mutableEvent);
      return;
    }
    // we need another local copy in case mutableEvent is modified in the DLQ
    Event eventCopy = event;
    logger.info("Retry attempts exhausted, routing message to DLQ: " + getUntilSuccessfulConfiguration().getDlqMP());
    try {
      RetryPolicyExhaustedException exception = buildRetryPolicyExhaustedException(lastException);

      Event mutatedEvent = Event.builder(mutableEvent)
          .message(InternalMessage.builder(mutableEvent.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception))
              .build())
          .error(ErrorBuilder.builder(exception).errorType(muleContext.getErrorTypeLocator().lookupErrorType(exception)).build())
          .build();

      getUntilSuccessfulConfiguration().getDlqMP().process(mutatedEvent);
    } catch (MessagingException e) {
      messagingExceptionHandler.handleException(e, eventCopy);
    } catch (Exception e) {
      messagingExceptionHandler.handleException(new MessagingException(event, e), eventCopy);
    }
  }

  protected RetryPolicyExhaustedException buildRetryPolicyExhaustedException(final Exception e) {
    MuleException muleException = ExceptionHelper.getRootMuleException(e);

    if (muleException == null) {
      return new RetryPolicyExhaustedException(CoreMessages.createStaticMessage(UNTIL_SUCCESSFUL_MSG_PREFIX, e.getMessage()),
                                               e, this);
    } else {
      // the logger processes only the inner-most MuleException, which should be a MessagingException. In order to not lose
      // information, we have to re-wrap its cause with this new exception.
      if (muleException.getCause() != null) {
        RetryPolicyExhaustedException retryPolicyExhaustedException = new RetryPolicyExhaustedException(CoreMessages
            .createStaticMessage(UNTIL_SUCCESSFUL_MSG_PREFIX, muleException.getMessage()),
                                                                                                        muleException.getCause());
        retryPolicyExhaustedException.getInfo().putAll(muleException.getInfo());
        return retryPolicyExhaustedException;
      } else {
        RetryPolicyExhaustedException retryPolicyExhaustedException = new RetryPolicyExhaustedException(CoreMessages
            .createStaticMessage(UNTIL_SUCCESSFUL_MSG_PREFIX, muleException.getMessage()),
                                                                                                        muleException);
        retryPolicyExhaustedException.getInfo().putAll(muleException.getInfo());
        return retryPolicyExhaustedException;
      }
    }
  }

  private void removeFromStore(final Serializable eventStoreKey) {
    try {
      getUntilSuccessfulConfiguration().getObjectStore().remove(eventStoreKey);
    } catch (final ObjectStoreException ose) {
      logger.warn("Failed to remove following event from store with key: " + eventStoreKey);
    }
  }

  private void retrieveAndProcessEvent(final Serializable eventStoreKey) throws ObjectStoreException {
    final Event persistedEvent = getUntilSuccessfulConfiguration().getObjectStore().retrieve(eventStoreKey);
    processEvent(persistedEvent);
    removeFromStore(eventStoreKey);
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
  }

}
