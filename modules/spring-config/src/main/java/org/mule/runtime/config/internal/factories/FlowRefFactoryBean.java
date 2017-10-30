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
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.config.internal.MuleArtifactContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
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

import javax.xml.namespace.QName;

import reactor.core.publisher.Mono;

public class FlowRefFactoryBean extends AbstractComponentFactory<Processor>
    implements ApplicationContextAware, MuleContextAware {

  private static final Logger LOGGER = getLogger(FlowRefFactoryBean.class);

  private String refName;
  private ApplicationContext applicationContext;
  private MuleContext muleContext;

  public void setName(String name) {
    this.refName = name;
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

    Processor referencedFlow = getReferencedProcessor(name);
    if (referencedFlow == null) {
      throw new RoutePathNotFoundException(createStaticMessage("No flow/sub-flow with name '%s' found", name),
                                           flowRefMessageProcessor);
    }

    // for subflows, we create a new one so it must be initialised manually
    if (!(referencedFlow instanceof Flow)) {
      Map<QName, Object> annotations = new HashMap<>(((Component) referencedFlow).getAnnotations());
      annotations.put(ROOT_CONTAINER_NAME_KEY, getRootContainerLocation().toString());
      ((Component) referencedFlow).setAnnotations(annotations);

      if (referencedFlow instanceof Initialisable) {
        initialiseIfNeeded(referencedFlow, muleContext);
        if (referencedFlow instanceof MessageProcessorChain) {
          for (Processor processor : ((MessageProcessorChain) referencedFlow).getMessageProcessors()) {
            setMuleContextIfNeeded(processor, muleContext);
          }
        }
      }
      startIfNeeded(referencedFlow);
    }

    return referencedFlow;
  }

  private Processor getReferencedProcessor(String name) {
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
    return (Processor) applicationContext.getBean(name);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
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

      this.isExpression = muleContext.getExpressionManager().isExpression(refName);
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

        // If referenced processor is a Flow use a child EventContext and wait for completion else simply compose
        if (referencedProcessor instanceof Flow) {
          return just(event)
              .flatMap(request -> Mono
                  .from(processWithChildContext(request, referencedProcessor,
                                                ofNullable(FlowRefFactoryBean.this.getLocation()),
                                                ((Flow) referencedProcessor).getExceptionListener())));
        } else {
          return just(event).transform(referencedProcessor);
        }
      });
    }

    protected Processor resolveReferencedProcessor(CoreEvent event) throws MuleException {
      String flowName;
      if (isExpression) {
        flowName = muleContext.getExpressionManager().parse(refName, event, getLocation());
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
