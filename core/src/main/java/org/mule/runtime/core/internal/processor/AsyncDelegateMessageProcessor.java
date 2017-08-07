/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.DefaultEventContext.fireAndForgetChild;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.construct.FlowConstruct.getFromAnnotatedObject;
import static org.mule.runtime.core.api.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.api.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.internal.util.ProcessingStrategyUtils.isSynchronousProcessing;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
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
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Scope;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.session.DefaultMuleSession;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes {@link Event}'s asynchronously using a {@link ProcessingStrategy} to schedule asynchronous processing of
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
  private FlowConstruct flowConstruct;

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
    flowConstruct = getFromAnnotatedObject(componentLocator, this);
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
  public Event process(Event event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher)
        .doOnNext(request -> just(request)
            .map(event -> asyncEvent(request))
            .transform(innerPublisher -> from(innerPublisher)
                .doOnNext(fireAsyncScheduledNotification())
                .doOnNext(asyncRequest -> just(asyncRequest)
                    .transform(scheduleAsync(delegate))
                    .doOnNext(event -> fireAsyncCompleteNotification(event, null))
                    .doOnError(MessagingException.class, e -> fireAsyncCompleteNotification(e.getEvent(), e))
                    .subscribe(event -> asyncRequest.getInternalContext().success(event),
                               throwable -> asyncRequest.getInternalContext().error(throwable))))
            .subscribe(requestUnbounded()));
  }


  private ReactiveProcessor scheduleAsync(Processor delegate) {
    if (!isSynchronousProcessing(flowConstruct) && flowConstruct instanceof Pipeline) {
      // If an async processing strategy is in use then use it to schedule async
      return publisher -> from(publisher).transform(((Pipeline) flowConstruct).getProcessingStrategy().onPipeline(delegate));
    } else {
      // Otherwise schedule async processing using IO pool.
      return publisher -> from(publisher).transform(delegate).subscribeOn(reactorScheduler);
    }
  }

  private Event asyncEvent(Event event) {
    // Clone event, make it async and remove ReplyToHandler
    return Event
        .builder(fireAndForgetChild(event.getInternalContext(), ofNullable(getLocation())), event)
        .replyToHandler(null)
        .session(new DefaultMuleSession(event.getSession())).build();
  }

  private Consumer<Event> fireAsyncScheduledNotification() {
    return event -> muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(createInfo(event, null, this), getLocation(), PROCESS_ASYNC_SCHEDULED));
  }

  private void fireAsyncCompleteNotification(Event event, MessagingException exception) {
    muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(createInfo(event, exception, this), getLocation(),
                                                       PROCESS_ASYNC_COMPLETE));
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(delegate);
  }

}
