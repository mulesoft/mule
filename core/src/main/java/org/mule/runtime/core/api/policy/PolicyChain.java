/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_START;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.scheduler.Schedulers.fromExecutor;
import static reactor.core.scheduler.Schedulers.immediate;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.policy.PolicyNotificationHelper;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.StreamPerEventSink;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Policy chain for handling the message processor associated to a policy.
 *
 * @since 4.0
 */
public class PolicyChain extends AbstractComponent
    implements Initialisable, Startable, Stoppable, Disposable, Processor {

  @Inject
  private MuleContext muleContext;

  @Inject
  private SchedulerService schedulerService;

  private List<Processor> processors;
  private MessageProcessorChain processorChain;

  private SourcePolicyProcessingStrategy processingStrategy;
  private ReactiveProcessor chainWithPs;
  private PolicyNotificationHelper notificationHelper;

  public void setProcessors(List<Processor> processors) {
    this.processors = processors;
  }

  @Override
  public final void initialise() throws InitialisationException {
    DefaultMessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder().chain(processors);

    List<Processor> beforeNext = new ArrayList<>();
    List<Processor> afterNext = new ArrayList<>();

    boolean seenNext = false;
    for (Processor processor : processors) {
      if (processor instanceof PolicyNextActionMessageProcessor) {
        seenNext = true;
        continue;
      }

      if (seenNext) {
        afterNext.add(processor);
      } else {
        beforeNext.add(processor);
      }
    }

    processingStrategy = new SourcePolicyProcessingStrategy(schedulerService, muleContext.getSchedulerBaseConfig(),
                                                            getLocation().getLocation(), beforeNext, afterNext);

    chainBuilder.setProcessingStrategy(processingStrategy);
    processorChain = chainBuilder.build();
    chainWithPs = processingStrategy.onPipeline(processorChain);
    initialiseIfNeeded(processorChain, muleContext);

    notificationHelper =
        new PolicyNotificationHelper(muleContext.getNotificationManager(), muleContext.getConfiguration().getId(), this);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processingStrategy);
    if (processorChain != null) {
      processorChain.start();
    }
  }

  @Override
  public void dispose() {
    if (processorChain != null) {
      processorChain.dispose();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (processorChain != null) {
      processorChain.stop();
    }
    stopIfNeeded(processingStrategy);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, chainWithPs);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return Mono.from(publisher)
        .doOnNext(notificationHelper.notification(PROCESS_START))
        .transform(chainWithPs)
        .doOnSuccess(notificationHelper.notification(PROCESS_END))
        .doOnError(MessagingException.class, notificationHelper.errorNotification(PROCESS_END));
  }

  /**
   * Will execute each of the 2 segments of a policy chain (the before and after segments relative to the {@code execute-next}
   * component) in its own scheduler, based on the 'heaviest' {@link ProcessingType} of its processors.
   */
  private static final class SourcePolicyProcessingStrategy extends AbstractProcessingStrategy
      implements ProcessingStrategy, Startable, Stoppable {

    private SchedulerService schedulerService;
    private SchedulerConfig schedulerBaseConfig;
    private String schedulersNamePrefix;
    private List<Processor> beforeNext;
    private List<Processor> afterNext;

    private Scheduler beforeScheduler;
    private Scheduler afterScheduler;
    private Scheduler cpuLiteBeforeScheduler;
    private Scheduler cpuLiteAfterScheduler;

    public SourcePolicyProcessingStrategy(SchedulerService schedulerService, SchedulerConfig schedulerBaseConfig,
                                          String schedulersNamePrefix, List<Processor> beforeNext, List<Processor> afterNext) {
      this.schedulerService = schedulerService;
      this.schedulerBaseConfig = schedulerBaseConfig;
      this.schedulersNamePrefix = schedulersNamePrefix;
      this.beforeNext = beforeNext;
      this.afterNext = afterNext;
    }

    @Override
    public void start() throws MuleException {
      beforeScheduler = schedulerBasedOnProcessingTypes(beforeNext, schedulersNamePrefix + "-before");
      afterScheduler = schedulerBasedOnProcessingTypes(afterNext, schedulersNamePrefix + "-after");
      cpuLiteBeforeScheduler =
          schedulerService.cpuLightScheduler(schedulerBaseConfig.withName(schedulersNamePrefix + "-cpuLite-before"));
      cpuLiteAfterScheduler =
          schedulerService.cpuLightScheduler(schedulerBaseConfig.withName(schedulersNamePrefix + "-cpuLite-after"));
    }

    private Scheduler schedulerBasedOnProcessingTypes(List<Processor> processors, String schedulerName) {
      Set<ProcessingType> types = processors.stream().map(p -> p.getProcessingType()).collect(toSet());

      SchedulerConfig config = schedulerBaseConfig.withName(schedulerName);
      if (types.stream().anyMatch(p -> BLOCKING.equals(p) || IO_RW.equals(p))) {
        return schedulerService.ioScheduler(config);
      } else if (types.stream().anyMatch(p -> CPU_INTENSIVE.equals(p))) {
        return schedulerService.cpuIntensiveScheduler(config);
      } else {
        return null;
      }
    }

    @Override
    public void stop() throws MuleException {
      if (beforeScheduler != null) {
        beforeScheduler.stop();
      }
      if (afterScheduler != null) {
        afterScheduler.stop();
      }
      if (cpuLiteBeforeScheduler != null) {
        cpuLiteBeforeScheduler.stop();
      }
      if (cpuLiteAfterScheduler != null) {
        cpuLiteAfterScheduler.stop();
      }
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      return new StreamPerEventSink(pipeline, createOnEventConsumer());
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      return publisher -> Flux.from(publisher)
          .publishOn(beforeScheduler != null ? fromExecutor(beforeScheduler) : immediate())
          .transform(pipeline);
    }


    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      ReactiveProcessor reactiveProcessor = handleCpuLiteAsync(processor);

      if (isRightAfterExecuteNext(processor)) {
        return publisher -> Flux.from(publisher)
            .publishOn(afterScheduler != null ? fromExecutor(afterScheduler) : immediate())
            .transform(reactiveProcessor);
      }

      return reactiveProcessor;
    }

    private boolean isRightAfterExecuteNext(ReactiveProcessor processor) {
      return !afterNext.isEmpty()
          && (processor.equals(afterNext.get(0))
              || (processor instanceof InterceptedReactiveProcessor
                  && ((InterceptedReactiveProcessor) processor).getProcessor().equals(afterNext.get(0))));
    }

    private ReactiveProcessor handleCpuLiteAsync(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {

        Scheduler scheduler;

        if (beforeNext.contains(processor instanceof InterceptedReactiveProcessor
            ? ((InterceptedReactiveProcessor) processor).getProcessor()
            : processor)) {
          scheduler = beforeScheduler != null ? beforeScheduler : cpuLiteBeforeScheduler;
        } else {
          scheduler = afterScheduler != null ? afterScheduler : cpuLiteAfterScheduler;
        }

        return publisher -> Flux.from(publisher)
            .transform(processor)
            .publishOn(fromExecutor(scheduler));
      } else {
        return processor;
      }
    }

  }
}
