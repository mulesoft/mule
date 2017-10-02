/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.api.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.api.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory.DIRECT_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.internal.component.ComponentUtils.getFromAnnotatedObject;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.api.notification.AsyncMessageNotification;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

/**
 * Processes {@link CoreEvent}'s asynchronously using a {@link ProcessingStrategy} to schedule asynchronous processing of
 * MessageProcessor delegate configured the next {@link Processor}. The next {@link Processor} is therefore be executed in a
 * different thread regardless of the exchange-pattern configured on the inbound endpoint. If a transaction is present then an
 * exception is thrown.
 */
public class AsyncDelegateMessageProcessor extends AbstractMessageProcessorOwner
    implements Scope, Initialisable, Startable, Stoppable {

  @Inject
  private SchedulerService schedulerService;
  @Inject
  private ConfigurationComponentLocator componentLocator;

  protected Logger logger = LoggerFactory.getLogger(getClass());
  private ProcessingStrategy processingStrategy;

  protected MessageProcessorChain delegate;
  private Scheduler scheduler;
  private reactor.core.scheduler.Scheduler reactorScheduler;
  protected String name;

  public AsyncDelegateMessageProcessor(MessageProcessorChain delegate) {
    this.delegate = delegate;
  }

  public AsyncDelegateMessageProcessor(MessageProcessorChain delegate, String name) {
    this.delegate = delegate;
    this.name = name;
  }

  @Override
  public void initialise() throws InitialisationException {
    Object rootContainer = getFromAnnotatedObject(componentLocator, this).orElse(null);
    if (rootContainer instanceof FlowConstruct) {
      processingStrategy = ((FlowConstruct) rootContainer).getProcessingStrategy();
    } else {
      processingStrategy = DIRECT_PROCESSING_STRATEGY_INSTANCE;
    }
    if (delegate == null) {
      throw new InitialisationException(objectIsNull("delegate message processor"), this);
    }
    super.initialise();
  }

  @Override
  public void start() throws MuleException {
    scheduler = schedulerService
        .ioScheduler(muleContext.getSchedulerBaseConfig().withName(name != null ? name : getLocation().getLocation()));
    reactorScheduler = fromExecutorService(scheduler);
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    super.stop();
    if (scheduler != null) {
      scheduler.stop();
      scheduler = null;
    }
    if (reactorScheduler != null) {
      reactorScheduler.dispose();
      reactorScheduler = null;
    }
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .cast(PrivilegedEvent.class)
        .doOnNext(request -> just(request)
            .map(event -> asyncEvent(request))
            .transform(innerPublisher -> from(innerPublisher)
                .doOnNext(fireAsyncScheduledNotification())
                .doOnNext(asyncRequest -> just(asyncRequest)
                    .cast(CoreEvent.class)
                    .transform(scheduleAsync(delegate))
                    .doOnNext(event -> fireAsyncCompleteNotification(event, null))
                    .doOnError(MessagingException.class, e -> fireAsyncCompleteNotification(e.getEvent(), e))
                    .doOnError(throwable -> logger
                        .warn("Error occurred during asynchronous processing at:" + getLocation().getLocation()
                            + " . To handle this error include a <try> scope in the <async> scope.",
                              throwable))
                    .subscribe(event -> asyncRequest.getContext().success(event),
                               throwable -> asyncRequest.getContext().error(throwable))))
            .subscribe(requestUnbounded()))
        .cast(CoreEvent.class);
  }


  private ReactiveProcessor scheduleAsync(Processor delegate) {
    if (!processingStrategy.isSynchronous()) {
      // If an async processing strategy is in use then use it to schedule async
      return publisher -> from(publisher).transform(processingStrategy.onPipeline(delegate));
    } else {
      // Otherwise schedule async processing using IO pool.
      return publisher -> from(publisher).transform(delegate).subscribeOn(reactorScheduler);
    }
  }

  private PrivilegedEvent asyncEvent(PrivilegedEvent event) {
    // Clone event, make it async and remove ReplyToHandler
    return PrivilegedEvent
        .builder(child((event.getContext()), ofNullable(getLocation())), event)
        .replyToHandler(null)
        .session(new DefaultMuleSession(event.getSession())).build();
  }

  private Consumer<CoreEvent> fireAsyncScheduledNotification() {
    return event -> muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(createInfo(event, null, this), getLocation(), PROCESS_ASYNC_SCHEDULED));
  }

  private void fireAsyncCompleteNotification(CoreEvent event, MessagingException exception) {
    muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(createInfo(event, exception, this), getLocation(),
                                                       PROCESS_ASYNC_COMPLETE));
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(delegate);
  }

}
