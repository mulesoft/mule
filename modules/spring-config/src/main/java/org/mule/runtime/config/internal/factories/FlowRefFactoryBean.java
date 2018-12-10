/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.util.rx.Operators.outputToTarget;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Stoppable;
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
import org.mule.runtime.core.internal.processor.chain.SubflowMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.routing.RoutePathNotFoundException;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import reactor.core.publisher.Flux;

public class FlowRefFactoryBean extends AbstractComponentFactory<Processor> implements ApplicationContextAware {

  private static final Logger LOGGER = getLogger(FlowRefFactoryBean.class);

  private String refName;
  private String target;
  private String targetValue = "#[payload]";

  private ApplicationContext applicationContext;
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
   * @param targetValue the target value expresion
   */
  public void setTargetValue(String targetValue) {
    this.targetValue = targetValue;
  }

  @Override
  public Processor doGetObject() throws Exception {
    if (refName.isEmpty()) {
      throw new IllegalArgumentException("flow-ref name is empty");
    }

    return new FlowRefMessageProcessor();
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

  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    try {
      muleContext.getInjector().inject(this);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private class FlowRefMessageProcessor extends AbstractComponent
      implements AnnotatedProcessor, Stoppable, Disposable {

    private LoadingCache<String, Processor> cache;
    private boolean isExpression;

    public FlowRefMessageProcessor() {
      this.cache = CacheBuilder.newBuilder()
          .maximumSize(20)
          .build(new CacheLoader<String, Processor>() {

            @Override
            public Processor load(String key) throws Exception {
              return getReferencedFlow(key, FlowRefMessageProcessor.this);
            }
          });

      this.isExpression = expressionManager.isExpression(refName);
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return processToApply(event, this);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher).flatMap(event -> {
        Processor referencedProcessor;
        try {
          referencedProcessor = resolveReferencedProcessor(event);
        } catch (MuleException e) {
          return error(e);
        }

        Flux<CoreEvent> flux;
        if (referencedProcessor instanceof Flow) {
          flux = from(processWithChildContext(event, referencedProcessor,
                                              ofNullable(FlowRefFactoryBean.this.getLocation()),
                                              ((Flow) referencedProcessor).getExceptionListener()));
        } else {
          flux = from(processWithChildContext(event, referencedProcessor,
                                              ofNullable(FlowRefFactoryBean.this.getLocation())));
        }
        return flux.map(outputToTarget(event, target, targetValue, expressionManager));
      });
    }

    protected Processor resolveReferencedProcessor(CoreEvent event) throws MuleException {
      String flowName;
      if (isExpression) {
        flowName = expressionManager.parse(refName, event, getLocation());
      } else {
        flowName = refName;
      }

      try {
        return cache.getUnchecked(flowName);

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
    public ComponentLocation getLocation() {
      return FlowRefFactoryBean.this.getLocation();
    }

    @Override
    public void stop() throws MuleException {
      for (Processor p : cache.asMap().values()) {
        if (!(p instanceof Flow)) {
          stopIfNeeded(p);
        }
      }
    }

    @Override
    public void dispose() {
      for (Processor p : cache.asMap().values()) {
        if (!(p instanceof Flow)) {
          disposeIfNeeded(p, LOGGER);
        }
      }
      cache.invalidateAll();
      cache.cleanUp();
    }

  }
}
