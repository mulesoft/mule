/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.util.rx.Operators.outputToTarget;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.config.internal.MuleArtifactContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.processor.chain.SubflowMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.routing.RoutePathNotFoundException;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FlowRefFactoryBean extends AbstractComponentFactory<Processor> implements ApplicationContextAware {

  private static final Logger LOGGER = getLogger(FlowRefFactoryBean.class);

  private String refName;
  private String target;
  private String targetValue = "#[payload]";

  private ApplicationContext applicationContext;

  @Inject
  private MuleContext muleContext;

  @Inject
  private ExtendedExpressionManager expressionManager;

  @Inject
  private ConfigurationComponentLocator locator;

  public void setName(String name) {
    this.refName = name;
  }

  /**
   * The variable where the result from this router should be stored. If this is not set then the result is set in the payload.
   *
   * @param target a variable name.
   */
  public void setTarget(String target) {
    this.target = target;
  }

  /**
   * Defines the target value expression
   *
   * @param targetValue the target value expression
   */
  public void setTargetValue(String targetValue) {
    this.targetValue = targetValue;
  }

  @Override
  public Processor doGetObject() throws Exception {
    if (refName.isEmpty()) {
      throw new IllegalArgumentException("flow-ref name is empty");
    }

    if (expressionManager.isExpression(refName)) {
      return new DynamicFlowRefMessageProcessor(this);
    } else {
      return new StaticFlowRefMessageProcessor(this);
    }
  }

  protected Processor getReferencedFlow(String name, FlowRefMessageProcessor flowRefMessageProcessor) throws MuleException {
    if (name == null) {
      throw new RoutePathNotFoundException(createStaticMessage("flow-ref name expression returned 'null'"),
                                           flowRefMessageProcessor);
    }

    Component referencedFlow = getReferencedProcessor(name);
    if (referencedFlow == null) {
      throw new RoutePathNotFoundException(createStaticMessage("No flow/sub-flow with name '%s' found", name),
                                           flowRefMessageProcessor);
    }

    // for subflows, we create a new one so it must be initialised manually
    if (!(referencedFlow instanceof Flow)) {
      if (referencedFlow instanceof SubflowMessageProcessorChainBuilder) {
        MessageProcessorChainBuilder chainBuilder = (MessageProcessorChainBuilder) referencedFlow;

        locator.find(flowRefMessageProcessor.getRootContainerLocation()).filter(c -> c instanceof Flow).map(c -> (Flow) c)
            .ifPresent(f -> {
              ProcessingStrategy callerFlowPs = f.getProcessingStrategy();
              chainBuilder.setProcessingStrategy(new ProcessingStrategy() {

                @Override
                public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
                  return callerFlowPs.createSink(flowConstruct, pipeline);
                }

                @Override
                public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
                  // Do not make any change in `onPipeline`, so it emulates the behavior of copy/pasting the content of the
                  // sub-flow into the caller flow, without applying any additional logic.
                  return pipeline;
                }

                @Override
                public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
                  return callerFlowPs.onProcessor(processor);
                }
              });
            });

        referencedFlow = chainBuilder.build();
      }
      initialiseIfNeeded(referencedFlow, muleContext);

      Map<QName, Object> annotations = new HashMap<>(referencedFlow.getAnnotations());
      annotations.put(ROOT_CONTAINER_NAME_KEY, getRootContainerLocation().toString());
      referencedFlow.setAnnotations(annotations);
      startIfNeeded(referencedFlow);
    }

    return (Processor) referencedFlow;
  }

  private Component getReferencedProcessor(String name) {
    if (applicationContext instanceof MuleArtifactContext) {
      MuleArtifactContext muleArtifactContext = (MuleArtifactContext) applicationContext;

      try {
        if (muleArtifactContext.getBeanFactory().getBeanDefinition(name).isPrototype()) {
          muleArtifactContext.getPrototypeBeanWithRootContainer(name, getRootContainerLocation().toString());
        }
      } catch (NoSuchBeanDefinitionException e) {
        // Null is handled by the caller method
        return null;
      }
    }
    return (Component) applicationContext.getBean(name);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * Flow-ref message processor with a statically (constant along the flow execution) defined target route.
   * 
   * @since 4.3.0
   */
  private class StaticFlowRefMessageProcessor extends FlowRefMessageProcessor {

    protected StaticFlowRefMessageProcessor(FlowRefFactoryBean owner) {
      super(owner);
    }

    @Override
    public void doStart() {}

    private final LazyValue<ReactiveProcessor> resolvedReferencedProcessorSupplier = new LazyValue<>(() -> {
      try {
        return getReferencedFlow(refName, StaticFlowRefMessageProcessor.this);
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    });

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      final ReactiveProcessor resolvedReferencedProcessor = resolvedReferencedProcessorSupplier.get();

      Flux<CoreEvent> pub = from(publisher);

      if (target != null) {
        pub = pub.map(event -> quickCopy(event, singletonMap(originalEventKey(event), event)))
            .cast(CoreEvent.class);
      }

      Optional<ComponentLocation> location = ofNullable(StaticFlowRefMessageProcessor.this.getLocation());

      if (resolvedReferencedProcessor instanceof Flow) {
        return applyForStaticFlow((Flow) resolvedReferencedProcessor, pub, location);
      } else {
        return applyForStaticSubFlow(resolvedReferencedProcessor, pub, location);
      }

    }

    private Publisher<CoreEvent> applyForStaticFlow(Flow resolvedTarget, Flux<CoreEvent> pub,
                                                    Optional<ComponentLocation> location) {
      pub = pub.transform(eventPub -> applyWithChildContext(eventPub, resolvedTarget.referenced(),
                                                                 location,
                                                                 resolvedTarget.getExceptionListener()));

      return (target != null)
          ? pub.map(eventAfter -> outputToTarget(((InternalEvent) eventAfter)
              .getInternalParameter(originalEventKey(eventAfter)), target, targetValue,
                                                 expressionManager).apply(eventAfter))
          : pub;
    }

    private Publisher<CoreEvent> applyForStaticSubFlow(ReactiveProcessor resolvedTarget, Flux<CoreEvent> pub,
                                                       Optional<ComponentLocation> location) {
      return pub.flatMap(event -> {
        ReactiveProcessor subFlowProcessor = publisher1 -> Mono.from(publisher1)
            .transform(resolvedTarget)
            .onErrorMap(MessagingException.class,
                        getMessagingExceptionMapper());
        return Mono
            .from(processWithChildContext(event, subFlowProcessor, location))
            .map(outputToTarget(event, target, targetValue, expressionManager));
      });
    }

    protected String originalEventKey(CoreEvent event) {
      return "flowRef.originalEvent." + event.getContext().getId();
    }

    @Override
    public void stop() throws MuleException {
      resolvedReferencedProcessorSupplier.ifComputed(resolved -> {
        if (!(resolved instanceof Flow)) {
          try {
            stopIfNeeded(resolved);
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        }
      });
    }

    @Override
    public void dispose() {
      resolvedReferencedProcessorSupplier.ifComputed(resolved -> {
        if (!(resolved instanceof Flow)) {
          disposeIfNeeded(resolved, LOGGER);
        }
      });
    }
  }

  /**
   * Flow-ref message processor whose route might change along the flow execution. This means the target route is defined with a
   * data-weave expression.
   * 
   * @since 4.3.0
   */
  private class DynamicFlowRefMessageProcessor extends FlowRefMessageProcessor {

    private LoadingCache<String, Processor> targetsCache;

    public DynamicFlowRefMessageProcessor(FlowRefFactoryBean owner) {
      super(owner);
      this.targetsCache = CacheBuilder.newBuilder()
          .maximumSize(20)
          .build(new CacheLoader<String, Processor>() {

            @Override
            public Processor load(String key) throws Exception {
              return getReferencedFlow(key, DynamicFlowRefMessageProcessor.this);
            }
          });
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher).flatMap(event -> {
        ReactiveProcessor resolvedTarget;

        try {
          resolvedTarget = resolveTargetFlowOrSubflow(event);
        } catch (MuleException e) {
          return error(e);
        }

        ReactiveProcessor decoratedTarget = p -> Mono.from(p)
            .transform(resolvedTarget)
            .onErrorMap(MessagingException.class,
                        getMessagingExceptionMapper());

        Optional<Flow> targetAsFlow = resolvedTarget instanceof Flow ? of((Flow) resolvedTarget) : empty();
        return Mono
            .from(processWithChildContextFlowOrSubflow(event, decoratedTarget, targetAsFlow))
            .map(outputToTarget(event, target, targetValue, expressionManager));
      });
    }

    protected Publisher<CoreEvent> processWithChildContextFlowOrSubflow(CoreEvent event,
                                                                        ReactiveProcessor decoratedTarget,
                                                                        Optional<Flow> targetAsFlow) {
      Optional<ComponentLocation> componentLocation = ofNullable(DynamicFlowRefMessageProcessor.this.getLocation());
      if (targetAsFlow.isPresent()) {
        return processWithChildContext(event, decoratedTarget,
                                       componentLocation,
                                       targetAsFlow.get().getExceptionListener());
      } else {
        // If the resolved target is not a flow, it should be a subflow
        return processWithChildContext(event, decoratedTarget, componentLocation);

      }
    }

    /**
     * Given the current {@link CoreEvent}, resolved which processor is targeted by it, being this a {@link Flow} or a
     * {@link org.mule.runtime.core.internal.processor.chain.SubflowMessageProcessorChainBuilder.SubFlowMessageProcessorChain}.
     * Also, caches the fetched {@link Processor} for future calls.
     * 
     * @param event the {@link CoreEvent} event
     * @return the {@link Processor} targeted by the current event
     * @throws MuleException
     */
    protected Processor resolveTargetFlowOrSubflow(CoreEvent event) throws MuleException {
      String targetFlowName = (String) expressionManager.evaluate(refName, event, getLocation()).getValue();
      try {
        return targetsCache.getUnchecked(targetFlowName);

      } catch (UncheckedExecutionException e) {
        if (e.getCause() instanceof MuleRuntimeException) {
          throw (MuleRuntimeException) e.getCause();
        } else if (e.getCause() instanceof MuleException) {
          throw (MuleException) e.getCause();
        } else {
          throw e;
        }
      }
    }

    @Override
    public void doStart() throws MuleException {
      for (Processor p : targetsCache.asMap().values()) {
        if (!(p instanceof Flow)) {
          startIfNeeded(p);
        }
      }
    }

    @Override
    public void stop() throws MuleException {
      for (Processor p : targetsCache.asMap().values()) {
        if (!(p instanceof Flow)) {
          stopIfNeeded(p);
        }
      }
    }

    @Override
    public void dispose() {
      for (Processor p : targetsCache.asMap().values()) {
        if (!(p instanceof Flow)) {
          disposeIfNeeded(p, LOGGER);
        }
      }
      targetsCache.invalidateAll();
      targetsCache.cleanUp();
    }

  }

  /**
   * Commonly used {@link MessagingException} mapper.
   * 
   * @return a {@link MessagingException} mapper that maps the input exception to one using the wrapped event's parent context.
   */
  private Function<MessagingException, Throwable> getMessagingExceptionMapper() {
    return me -> new MessagingException(quickCopy(((BaseEventContext) me.getEvent().getContext()).getParentContext()
        .get(), me.getEvent()), me);
  }
}
