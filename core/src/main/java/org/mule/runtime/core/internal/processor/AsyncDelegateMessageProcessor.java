/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.yield;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.api.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.component.ComponentUtils.getFromAnnotatedObject;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.notification.AsyncMessageNotification;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.LoggingExceptionHandler;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.internal.construct.FromFlowRejectedExecutionException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;

/**
 * Processes {@link CoreEvent}'s asynchronously using a {@link ProcessingStrategy} to schedule asynchronous processing of
 * MessageProcessor delegate configured the next {@link Processor}. The next {@link Processor} is therefore be executed in a
 * different thread regardless of the exchange-pattern configured on the inbound endpoint. If a transaction is present then an
 * exception is thrown.
 */
public class AsyncDelegateMessageProcessor extends AbstractMessageProcessorOwner
    implements Scope, Initialisable, Startable, Stoppable {

  @Inject
  private MuleContext muleContext;
  @Inject
  private SchedulerService schedulerService;
  @Inject
  private ConfigurationComponentLocator componentLocator;

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private ProcessingStrategy processingStrategy;

  private Sink sink;

  private QueueBackpressureHandler backpressureHandler;

  private final MessageProcessorChainBuilder delegateBuilder;
  protected MessageProcessorChain delegate;
  private Scheduler reactorScheduler;
  protected String name;
  private Integer maxConcurrency;

  public AsyncDelegateMessageProcessor(MessageProcessorChainBuilder delegate) {
    this.delegateBuilder = delegate;
  }

  public AsyncDelegateMessageProcessor(MessageProcessorChainBuilder delegate, String name) {
    this.delegateBuilder = delegate;
    this.name = name;
  }

  @Override
  public void initialise() throws InitialisationException {
    Component rootContainer = getFromAnnotatedObject(componentLocator, this).orElse(null);
    if (rootContainer instanceof Pipeline) {
      if (maxConcurrency != null) {
        ProcessingStrategyFactory flowPsFactory = ((Pipeline) rootContainer).getProcessingStrategyFactory();

        if (flowPsFactory instanceof AsyncProcessingStrategyFactory) {
          ((AsyncProcessingStrategyFactory) flowPsFactory).setMaxConcurrency(maxConcurrency);
        } else {
          logger.warn("{} does not support 'maxConcurrency'. Ignoring the value.", flowPsFactory.getClass().getSimpleName());
        }
        processingStrategy = flowPsFactory.create(getMuleContext(), getLocation().getLocation());
      } else {
        ProcessingStrategyFactory flowPsFactory = ((Pipeline) rootContainer).getProcessingStrategyFactory();
        processingStrategy = flowPsFactory.create(getMuleContext(), getLocation().getLocation());
      }
    } else {
      processingStrategy = createDefaultProcessingStrategyFactory().create(getMuleContext(), getLocation().getLocation());
    }
    if (delegateBuilder == null) {
      throw new InitialisationException(objectIsNull("delegate message processor"), this);
    }

    // TODO MULE-17020 Interception API: Smart connectors inside async are not skipped properly
    delegateBuilder.setProcessingStrategy(processingStrategy);
    delegate = delegateBuilder.build();
    initialiseIfNeeded(delegate, getMuleContext());

    backpressureHandler = new QueueBackpressureHandler(schedulerService, () -> muleContext.getSchedulerBaseConfig(),
                                                       this::dispatchEvent, name != null ? name : getLocation().getLocation());

    super.initialise();
  }

  /**
   * A fallback method for creating a {@link ProcessingStrategyFactory}.
   *
   * @return a {@link DirectProcessingStrategyFactory}
   */
  protected ProcessingStrategyFactory createDefaultProcessingStrategyFactory() {
    return new DirectProcessingStrategyFactory();
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processingStrategy);
    startIfNeeded(delegate);

    sink = processingStrategy
        .createSink(getFromAnnotatedObject(componentLocator, this).filter(c -> c instanceof FlowConstruct).orElse(null),
                    processAsyncChainFunction());

    final SchedulerConfig schedulerConfig =
        getMuleContext().getSchedulerBaseConfig().withName(name != null ? name : getLocation().getLocation());
    reactorScheduler = processingStrategy.isSynchronous()
        ? schedulerService.ioScheduler(schedulerConfig)
        : schedulerService.cpuLightScheduler(schedulerConfig);

    startIfNeeded(backpressureHandler);
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    super.stop();

    safely(() -> stopIfNeeded(backpressureHandler));
    disposeIfNeeded(sink, logger);
    sink = null;
    stopIfNeeded(delegate);

    if (reactorScheduler != null) {
      reactorScheduler.stop();
      reactorScheduler = null;
    }
    stopIfNeeded(processingStrategy);
  }

  @Override
  public void dispose() {
    super.dispose();
    disposeIfNeeded(delegate, logger);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .cast(PrivilegedEvent.class)
        .doOnNext(request -> {

          Flux<CoreEvent> asyncPublisher = just(request)
              .map(event -> asyncEvent(event));

          if (isTransactionActive() && !processingStrategy.isSynchronous()) {
            asyncPublisher = asyncPublisher.publishOn(fromExecutorService(reactorScheduler));
          }

          asyncPublisher
              .map(event -> {
                try {
                  dispatchEvent(event);
                } catch (FromFlowRejectedExecutionException free) {
                  backpressureHandler.handleBackpressure(event);
                }
                return event;
              })
              .subscribe(requestUnbounded());
        })
        .cast(CoreEvent.class);
  }

  private void dispatchEvent(CoreEvent event) {
    processingStrategy.checkBackpressureAccepting(event);
    sink.accept(event);
  }

  private CoreEvent asyncEvent(PrivilegedEvent event) {
    // Clone event, make it async and remove ReplyToHandler
    return PrivilegedEvent
        .builder(child((event.getContext()), ofNullable(getLocation()), LoggingExceptionHandler.getInstance()), event)
        .session(new DefaultMuleSession(event.getSession())).build();
  }

  private ReactiveProcessor processAsyncChainFunction() {
    return innerPublisher -> from(innerPublisher)
        .doOnNext(fireAsyncScheduledNotification())
        .transform(processingStrategy.onPipeline(scheduleAsync(delegate)))
        .doOnNext(event -> {
          fireAsyncCompleteNotification(event, null);
          ((BaseEventContext) event.getContext()).success(event);
        })
        .doOnError(MessagingException.class, e -> {
          fireAsyncCompleteNotification(e.getEvent(), e);
          ((BaseEventContext) e.getEvent().getContext()).error(e);
        })
        .doOnError(throwable -> logger.warn("Error occurred during asynchronous processing at:" + getLocation().getLocation()
            + " . To handle this error include a <try> scope in the <async> scope.", throwable));
  }

  private ReactiveProcessor scheduleAsync(Processor delegate) {
    if (processingStrategy.isSynchronous()) {
      // schedule async processing using IO pool.
      return publisher -> from(publisher).transform(delegate).subscribeOn(fromExecutorService(reactorScheduler));
    } else {
      return delegate;
    }
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

  public void setMaxConcurrency(Integer maxConcurrency) {
    this.maxConcurrency = maxConcurrency;
  }

  @Override
  protected List<Processor> getOwnedObjects() {
    // Lifecycle of inner objects is already handled by this class' lifecycle methods
    return emptyList();
  }

  private static class QueueBackpressureHandler implements Stoppable {

    private final BlockingQueue<CoreEvent> asyncQueue;
    private final Consumer<CoreEvent> eventDispatcher;

    private final LazyValue<Scheduler> queueDispatcherScheduler;
    private final AtomicReference<Future> executing = new AtomicReference<>();

    public QueueBackpressureHandler(SchedulerService schedulerService, Supplier<SchedulerConfig> schedulerConfigSupplier,
                                    Consumer<CoreEvent> eventDispatcher, String location) {
      this.asyncQueue = new LinkedBlockingQueue<>();
      this.eventDispatcher = eventDispatcher;

      this.queueDispatcherScheduler = new LazyValue(() -> {
        final SchedulerConfig schedulerConfig = schedulerConfigSupplier.get().withName(location + " - queue dispatcher")
            .withMaxConcurrentTasks(1);
        return schedulerService.customScheduler(schedulerConfig);
      });
    }

    private Future dispatchTask() {
      return queueDispatcherScheduler.get().submit(() -> {
        while (!currentThread().isInterrupted()) {
          try {
            final CoreEvent queuedEvent = asyncQueue.peek();
            if (queuedEvent != null) {
              eventDispatcher.accept(queuedEvent);
              asyncQueue.remove(queuedEvent);
            } else {
              synchronized (executing) {
                if (asyncQueue.size() == 0) {
                  executing.set(null);
                  return;
                }
              }
            }
          } catch (FromFlowRejectedExecutionException free) {
            // Nothing to do, let next iteration catch it.
            yield();
          }
        }
      });
    }

    public void handleBackpressure(CoreEvent event) {
      asyncQueue.offer(event);

      synchronized (executing) {
        if (executing.get() == null) {
          executing.set(dispatchTask());
        }
      }
    }

    @Override
    public void stop() {
      queueDispatcherScheduler.ifComputed(Scheduler::stop);

      asyncQueue.clear();
    }
  }

  void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

}
