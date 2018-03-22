/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

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
import static reactor.core.publisher.Mono.error;
import static reactor.core.scheduler.Schedulers.fromExecutor;
import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
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
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.lifecycle.DefaultLifecycleManager;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;
import org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.StreamPerEventSink;

import java.util.Optional;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;

@NoExtend
public class DefaultPolicyInstance extends AbstractComponent
    implements PolicyInstance, FlowConstruct, MuleContextAware, Lifecycle {

  private final static Logger logger = getLogger(DefaultPolicyInstance.class);

  @Inject
  private SchedulerService schedulerService;

  private ProcessingStrategy processingStrategy;

  private PolicyChain operationPolicyChain;
  private PolicyChain sourcePolicyChain;

  private FlowConstructStatistics flowConstructStatistics = new DefaultFlowConstructStatistics("policy", getName());
  private String name = "proxy-policy-" + UUID.getUUID();
  private MuleContext muleContext;
  private DefaultLifecycleManager lifecycleStateManager = new DefaultLifecycleManager(this.name, this);

  @Override
  public void initialise() throws InitialisationException {
    processingStrategy =
        new PolicyProcessingStrategy(schedulerService, muleContext.getSchedulerBaseConfig(), getLocation().getLocation());

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
    disposeIfNeeded(operationPolicyChain, logger);
    disposeIfNeeded(sourcePolicyChain, logger);
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
      implements ProcessingStrategy, Startable, Stoppable {

    private SchedulerService schedulerService;
    private SchedulerConfig schedulerBaseConfig;
    private String schedulersNamePrefix;

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

    @Override
    public void stop() throws MuleException {
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
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (isExecuteNext(processor)) {
        return publisher -> Flux.from(publisher)
            .transform(processor);
      } else if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> Flux.from(publisher)
            .transform(processor)
            .publishOn(fromExecutor(cpuLiteScheduler));
      } else if (processor.getProcessingType() == IO_RW || processor.getProcessingType() == BLOCKING) {
        return publisher -> Flux.from(publisher)
            .publishOn(fromExecutor(ioScheduler))
            .transform(processor);
      } else if (processor.getProcessingType() == CPU_INTENSIVE) {
        return publisher -> Flux.from(publisher)
            .publishOn(fromExecutor(cpuIntensiveScheduler))
            .transform(processor);
      } else {
        return publisher -> Flux.from(publisher)
            .transform(processor);
      }
    }

    private boolean isExecuteNext(ReactiveProcessor processor) {
      return (processor instanceof PolicyNextActionMessageProcessor)
          || (processor instanceof InterceptedReactiveProcessor
              && ((InterceptedReactiveProcessor) processor).getProcessor() instanceof PolicyNextActionMessageProcessor);
    }

  }
}
