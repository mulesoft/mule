/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import static org.mule.runtime.core.DefaultEventContext.child;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContextHandleErrors;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.util.rx.Operators;
import org.mule.runtime.core.processor.AnnotatedProcessor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.Map;

import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import reactor.core.publisher.Mono;

public class FlowRefFactoryBean extends AbstractAnnotatedObject
    implements FactoryBean<Processor>, ApplicationContextAware, MuleContextAware {

  private static final Logger LOGGER = getLogger(FlowRefFactoryBean.class);

  private String refName;
  private ApplicationContext applicationContext;
  private MuleContext muleContext;

  public void setName(String name) {
    this.refName = name;
  }

  @Override
  public Processor getObject() throws Exception {
    if (refName.isEmpty()) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull("flow reference is empty"));
    }

    return new FlowRefMessageProcessor();
  }

  private Event createChildEvent(Event event) {
    return builder(child(event.getContext()), event).build();
  }

  private Event createParentEvent(Event parent, Event result) {
    return builder(parent.getContext(), result).build();
  }

  protected Processor getReferencedFlow(String name, FlowConstruct flowConstruct) throws MuleException {
    if (name == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
    }

    Processor referencedFlow = ((Processor) applicationContext.getBean(name));
    if (referencedFlow == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
    }

    // for subflows, we create a new one so it must be initialised manually
    if (!(referencedFlow instanceof Flow)) {
      if (referencedFlow instanceof AnnotatedObject) {
        ((AnnotatedObject) referencedFlow).setAnnotations(getAnnotations());
      }
      if (referencedFlow instanceof Initialisable) {
        prepareProcessor(referencedFlow, flowConstruct);
        if (referencedFlow instanceof MessageProcessorChain) {
          for (Processor processor : ((MessageProcessorChain) referencedFlow).getMessageProcessors()) {
            prepareProcessor(processor, flowConstruct);
          }
        }
        initialiseIfNeeded(referencedFlow);
      }
      startIfNeeded(referencedFlow);
    }

    return referencedFlow;
  }

  private void prepareProcessor(Processor p, FlowConstruct flowConstruct) {
    if (p instanceof FlowConstructAware) {
      ((FlowConstructAware) p).setFlowConstruct(flowConstruct);
    }
    if (p instanceof MuleContextAware) {
      ((MuleContextAware) p).setMuleContext(muleContext);
    }
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public Class<?> getObjectType() {
    return Processor.class;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  private class FlowRefMessageProcessor
      implements AnnotatedProcessor, FlowConstructAware, Stoppable, Disposable {

    private FlowConstruct flowConstruct;
    private LoadingCache<String, Processor> cache;
    private boolean isExpression;

    public FlowRefMessageProcessor() {
      this.cache = CacheBuilder.newBuilder()
          .maximumSize(20)
          .build(new CacheLoader<String, Processor>() {

            @Override
            public Processor load(String key) throws Exception {
              return getReferencedFlow(key, flowConstruct);
            }
          });

      this.isExpression = muleContext.getExpressionManager().isExpression(refName);
    }

    @Override
    public Event process(Event event) throws MuleException {
      return processToApply(event, this);
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
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
              .flatMap(request -> Mono.from(processWithChildContextHandleErrors(request, referencedProcessor))
                  .doOnSuccess(result -> {
                    if (result == null) {
                      event.getContext().success();
                    }
                  }));
        } else {
          return just(event).transform(referencedProcessor);
        }
      });
    }

    protected Processor resolveReferencedProcessor(Event event) throws MuleException {
      String flowName;
      if (isExpression) {
        flowName = muleContext.getExpressionManager().parse(refName, event, flowConstruct);
      } else {
        flowName = refName;
      }

      try {
        return cache.getUnchecked(flowName);

      } catch (UncheckedExecutionException e) {
        if (e.getCause() instanceof MuleRuntimeException) {
          throw (MuleRuntimeException) e.getCause();
        } else {
          throw e;
        }
      }
    }

    @Override
    public Object getAnnotation(QName name) {
      return FlowRefFactoryBean.this.getAnnotation(name);
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return FlowRefFactoryBean.this.getAnnotations();
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {
      FlowRefFactoryBean.this.setAnnotations(annotations);
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct) {
      this.flowConstruct = flowConstruct;
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
