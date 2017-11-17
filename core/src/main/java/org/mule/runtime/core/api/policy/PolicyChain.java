/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_START;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
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
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.StreamPerEventSink;
import org.mule.runtime.core.privileged.processor.MessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

import java.util.HashSet;
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

    Set<ProcessingType> beforeNext = new HashSet<>();
    Set<ProcessingType> afterNext = new HashSet<>();
    Processor followingNext = null;

    boolean seenNext = false;
    for (Processor processor : processors) {
      if (processor instanceof PolicyNextActionMessageProcessor) {
        seenNext = true;
        continue;
      }

      if (seenNext) {
        if (followingNext == null) {
          followingNext = processor;
        }
        afterNext.add(processor.getProcessingType());
      } else {
        beforeNext.add(processor.getProcessingType());
      }
    }

    processingStrategy = new SourcePolicyProcessingStrategy(schedulerService, muleContext.getSchedulerBaseConfig(),
                                                            beforeNext, afterNext, followingNext);

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
    return MessageProcessors.processToApply(event, chainWithPs);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return Mono.from(publisher)
        .doOnNext(notificationHelper.notification(PROCESS_START))
        .transform(chainWithPs)
        .doOnSuccess(notificationHelper.notification(PROCESS_END))
        .doOnError(MessagingException.class, notificationHelper.errorNotification(PROCESS_END));
  }

  private static final class SourcePolicyProcessingStrategy extends AbstractProcessingStrategy
      implements ProcessingStrategy, Startable, Stoppable {

    private SchedulerService schedulerService;
    private SchedulerConfig schedulerBaseConfig;
    private Set<ProcessingType> beforeNext;
    private Set<ProcessingType> afterNext;
    private Processor followingNext;

    private Scheduler beforeScheduler;
    private Scheduler afterScheduler;

    public SourcePolicyProcessingStrategy(SchedulerService schedulerService, SchedulerConfig schedulerBaseConfig,
                                          Set<ProcessingType> beforeNext, Set<ProcessingType> afterNext,
                                          Processor followingNext) {
      this.schedulerService = schedulerService;
      this.schedulerBaseConfig = schedulerBaseConfig;
      this.beforeNext = beforeNext;
      this.afterNext = afterNext;
      this.followingNext = followingNext;
    }

    @Override
    public void start() throws MuleException {
      SchedulerConfig beforeConfig = schedulerBaseConfig.withName("policy-source-before");
      if (beforeNext.stream().allMatch(p -> CPU_LITE_ASYNC.equals(p))) {
        beforeScheduler = schedulerService.cpuLightScheduler(beforeConfig);
      } else if (beforeNext.stream().anyMatch(p -> BLOCKING.equals(p) || IO_RW.equals(p))) {
        beforeScheduler = schedulerService.ioScheduler(beforeConfig);
      } else if (beforeNext.stream().anyMatch(p -> CPU_INTENSIVE.equals(p))) {
        beforeScheduler = schedulerService.cpuIntensiveScheduler(beforeConfig);
      }

      SchedulerConfig afterConfig = schedulerBaseConfig.withName("policy-source-after");
      if (afterNext.stream().allMatch(p -> CPU_LITE_ASYNC.equals(p))) {
        afterScheduler = schedulerService.cpuLightScheduler(afterConfig);
      } else if (afterNext.stream().anyMatch(p -> BLOCKING.equals(p) || IO_RW.equals(p))) {
        afterScheduler = schedulerService.ioScheduler(afterConfig);
      } else if (afterNext.stream().anyMatch(p -> CPU_INTENSIVE.equals(p))) {
        afterScheduler = schedulerService.cpuIntensiveScheduler(afterConfig);
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
      if (processor.equals(followingNext) || (processor instanceof InterceptedReactiveProcessor
          && ((InterceptedReactiveProcessor) processor).getProcessor().equals(followingNext))) {
        return publisher -> Flux.from(publisher)
            .publishOn(afterScheduler != null ? fromExecutor(afterScheduler) : immediate())
            .transform(processor);
      } else {
        return processor;
      }
    }

  }
}
