/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.time.Duration.ofMillis;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.error;
import static reactor.core.scheduler.Schedulers.fromExecutor;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import static reactor.retry.Retry.onlyIf;
import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.policy.PolicyInstance;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.lifecycle.DefaultLifecycleManager;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.StreamPerEventSink;

import java.util.Optional;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.retry.BackoffDelay;

@NoExtend
public class DefaultPolicyInstance extends AbstractComponent
    implements PolicyInstance, FlowConstruct, MuleContextAware, Lifecycle {

  private static final Logger LOGGER = getLogger(DefaultPolicyInstance.class);

  @Inject
  private SchedulerService schedulerService;

  private ProcessingStrategy processingStrategy;

  private String name;

  private PolicyChain operationPolicyChain;
  private PolicyChain sourcePolicyChain;

  private FlowConstructStatistics flowConstructStatistics;
  private MuleContext muleContext;
  private final DefaultLifecycleManager<DefaultPolicyInstance> lifecycleStateManager =
      new DefaultLifecycleManager<>("proxy-policy-" + UUID.getUUID(), this);

  @Override
  public void initialise() throws InitialisationException {
    flowConstructStatistics = new DefaultFlowConstructStatistics("policy", getName());

    processingStrategy =
        new PolicyProcessingStrategy(schedulerService, muleContext.getSchedulerBaseConfig(), getName());

    if (operationPolicyChain != null) {
      operationPolicyChain.setProcessingStrategy(processingStrategy);
    }
    if (sourcePolicyChain != null) {
      sourcePolicyChain.setProcessingStrategy(processingStrategy);
    }

    initialiseIfNeeded(operationPolicyChain, muleContext);
    initialiseIfNeeded(sourcePolicyChain, muleContext);
    lifecycleStateManager.fireInitialisePhase((phaseNam, object) -> {
    });
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processingStrategy);
    startIfNeeded(operationPolicyChain);
    startIfNeeded(sourcePolicyChain);
    lifecycleStateManager.fireStartPhase((phaseNam, object) -> {
    });

  }

  @Override
  public FlowExceptionHandler getExceptionListener() {
    return new FlowExceptionHandler() {

      @Override
      public CoreEvent handleException(Exception exception, CoreEvent event) {
        return null;
      }

      @Override
      public Publisher<CoreEvent> apply(Exception exception) {
        return error(exception);
      }
    };
  }

  @Override
  public FlowConstructStatistics getStatistics() {
    return this.flowConstructStatistics;
  }

  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public String getUniqueIdString() {
    return muleContext.getUniqueIdString();
  }

  @Override
  public String getServerId() {
    return muleContext.getId();
  }

  @Override
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public LifecycleState getLifecycleState() {
    return lifecycleStateManager.getState();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void dispose() {
    disposeIfNeeded(operationPolicyChain, LOGGER);
    disposeIfNeeded(sourcePolicyChain, LOGGER);
    disposeIfNeeded(processingStrategy, LOGGER);
    lifecycleStateManager.fireDisposePhase((phaseNam, object) -> {
    });
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(operationPolicyChain);
    stopIfNeeded(sourcePolicyChain);
    stopIfNeeded(processingStrategy);
    lifecycleStateManager.fireStopPhase((phaseNam, object) -> {
    });
  }

  public void setOperationPolicyChain(PolicyChain request) {
    this.operationPolicyChain = request;
  }

  public void setSourcePolicyChain(PolicyChain source) {
    this.sourcePolicyChain = source;
  }

  @Override
  public Optional<PolicyChain> getSourcePolicyChain() {
    return ofNullable(sourcePolicyChain);
  }

  @Override
  public Optional<PolicyChain> getOperationPolicyChain() {
    return ofNullable(operationPolicyChain);
  }

  @Override
  public ProcessingStrategy getProcessingStrategy() {
    return processingStrategy;
  }

  /**
   * Will execute each of the 2 segments of a policy chain (the before and after segments relative to the {@code execute-next}
   * component) in its own scheduler, based on the 'heaviest' {@link ProcessingType} of its processors.
   */
  private static final class PolicyProcessingStrategy extends AbstractProcessingStrategy
      implements ProcessingStrategy, Startable, Disposable {

    private static final Logger LOGGER = getLogger(PolicyProcessingStrategy.class);

    private final SchedulerService schedulerService;
    private final SchedulerConfig schedulerBaseConfig;
    private final String schedulersNamePrefix;

    private Scheduler ioScheduler;
    private Scheduler cpuIntensiveScheduler;

    private Scheduler cpuLiteScheduler;

    public PolicyProcessingStrategy(SchedulerService schedulerService, SchedulerConfig schedulerBaseConfig,
                                    String schedulersNamePrefix) {
      this.schedulerService = schedulerService;
      this.schedulerBaseConfig = schedulerBaseConfig;
      this.schedulersNamePrefix = schedulersNamePrefix;
    }

    @Override
    public void start() throws MuleException {
      ioScheduler = schedulerService.ioScheduler(schedulerBaseConfig.withName(schedulersNamePrefix));
      cpuIntensiveScheduler = schedulerService.cpuIntensiveScheduler(schedulerBaseConfig.withName(schedulersNamePrefix));
      cpuLiteScheduler = schedulerService.cpuLightScheduler(schedulerBaseConfig.withName(schedulersNamePrefix + "-cpuLite"));
    }

    public void stopSchedulers() {
      if (cpuLiteScheduler != null) {
        cpuLiteScheduler.stop();
      }
      if (cpuIntensiveScheduler != null) {
        cpuIntensiveScheduler.stop();
      }
      if (ioScheduler != null) {
        ioScheduler.stop();
      }
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      return new StreamPerEventSink(pipeline, createOnEventConsumer());
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      return publisher -> from(publisher)
          .transform(super.onPipeline(pipeline))
          .doOnComplete(() -> stopSchedulers());
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (isExecuteNext(processor)) {
        return super.onProcessor(processor);
      } else if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return switchBackThreadPool(processor);
      } else if (processor.getProcessingType() == IO_RW || processor.getProcessingType() == BLOCKING) {
        return switchThreadPool(processor, ioScheduler);
      } else if (processor.getProcessingType() == CPU_INTENSIVE) {
        return switchThreadPool(processor, cpuIntensiveScheduler);
      } else {
        return super.onProcessor(processor);
      }
    }

    private boolean isExecuteNext(ReactiveProcessor processor) {
      return (processor instanceof PolicyNextActionMessageProcessor)
          || (processor instanceof InterceptedReactiveProcessor
              && ((InterceptedReactiveProcessor) processor).getProcessor() instanceof PolicyNextActionMessageProcessor);
    }

    private ReactiveProcessor switchBackThreadPool(ReactiveProcessor processor) {
      return publisher -> from(publisher)
          .transform(processor)
          .publishOn(fromExecutorService(cpuLiteScheduler))
          .retryWhen(onlyIf(ctx -> {
            final boolean schedulerBusy = isSchedulerBusy(ctx.exception());
            if (schedulerBusy) {
              LOGGER.trace("Shared scheduler {} is busy. Scheduling of the current event will be retried after {}ms.",
                           cpuLiteScheduler.getName(), SCHEDULER_BUSY_RETRY_INTERVAL_MS);
            }
            return schedulerBusy;
          })
              .backoff(ctx -> new BackoffDelay(ofMillis(SCHEDULER_BUSY_RETRY_INTERVAL_MS)))
              .withBackoffScheduler(fromExecutorService(cpuLiteScheduler)));
    }

    private ReactiveProcessor switchThreadPool(ReactiveProcessor processor, Scheduler targetScheduler) {
      return publisher -> from(publisher)
          .publishOn(fromExecutor(targetScheduler))
          .transform(processor)
          .retryWhen(onlyIf(ctx -> {
            final boolean schedulerBusy = isSchedulerBusy(ctx.exception());
            if (schedulerBusy) {
              LOGGER.trace("Shared scheduler {} is busy. Scheduling of the current event will be retried after {}ms.",
                           targetScheduler.getName(), SCHEDULER_BUSY_RETRY_INTERVAL_MS);
            }
            return schedulerBusy;
          })
              .backoff(ctx -> new BackoffDelay(ofMillis(SCHEDULER_BUSY_RETRY_INTERVAL_MS)))
              .withBackoffScheduler(fromExecutorService(cpuLiteScheduler)));
    }

    @Override
    public void dispose() {
      stopSchedulers();
    }
  }
}
