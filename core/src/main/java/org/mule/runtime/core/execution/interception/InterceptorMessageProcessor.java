/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.execution.interception;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.util.rx.internal.Operators.nullSafeMap;
import static org.mule.runtime.dsl.api.component.ComponentIdentifier.ANNOTATION_NAME;
import static org.mule.runtime.dsl.api.component.ComponentIdentifier.ANNOTATION_PARAMETERS;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.interception.ProcessorInterceptorCallback;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor implementation that uses {@link org.mule.runtime.core.api.interception.ProcessorInterceptionManager} to retrieve
 * the {@link org.mule.runtime.core.api.interception.ProcessorInterceptorCallback} in order to delegate the interception of the
 * {@link Processor} that is intercepted.
 * <p/>
 * It is also responsible for continue the chain execution if the {@link Processor} intercepted is an {@link Interceptor} and
 * is responsible for {@link Lifecycle} of the intercepted {@link Processor}.
 * <p/>
 * It relies on {@link org.mule.runtime.api.meta.AnnotatedObject} to get the information for intercepting.
 * In case of ObjectFactories it will be populated with the intercepted annotations.
 *
 * @since 4.0
 */
public class InterceptorMessageProcessor extends AbstractAnnotatedObject implements Processor, FlowConstructAware,
    MuleContextAware, Lifecycle, Interceptor {

  private transient Logger logger = LoggerFactory.getLogger(InterceptorMessageProcessor.class);

  @Inject
  private MuleContext muleContext;
  private FlowConstruct flowConstruct;
  private Processor intercepted;
  private Processor next;

  /**
   * Sets the {@link Processor} being intercepted.
   *
   * @param intercepted {@link Processor} being intercepted.
   */
  public void setIntercepted(Processor intercepted) {
    this.intercepted = intercepted;
  }

  /**
   * {@inheritDoc}
   */
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setListener(Processor listener) {
    this.next = listener;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    if (intercepted == null) {
      throw new InitialisationException(createStaticMessage("No processor has been set to be intercepted"), this);
    }
    if (this.getAnnotations().get(ANNOTATION_NAME) == null) {
      throw new InitialisationException(createStaticMessage("Intercepted processor: " + intercepted.getClass().getName()
          + " but no annotations where defined for it"), this);
    }
    if (intercepted instanceof FlowConstructAware) {
      ((FlowConstructAware) intercepted).setFlowConstruct(flowConstruct);
    }
    initialiseIfNeeded(intercepted, true, muleContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() throws MuleException {
    startIfNeeded(intercepted);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() throws MuleException {
    stopIfNeeded(intercepted);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    disposeIfNeeded(intercepted, logger);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  /**
   * @return the {@link ProcessingType} of the intercepted {@link Processor}.
   */
  @Override
  public ProcessingType getProcessingType() {
    return intercepted.getProcessingType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Event process(Event event) throws MuleException {
    final ComponentIdentifier componentIdentifier = (ComponentIdentifier) this.getAnnotations().get(ANNOTATION_NAME);
    Map<String, Object> parameters = ((Map) this.getAnnotations().getOrDefault(ANNOTATION_PARAMETERS, emptyMap()));
    Map<String, Object> resolvedParameters = new HashMap<>();
    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      Object value;
      String paramValue = (String) entry.getValue();
      if (muleContext.getExpressionManager().isExpression(paramValue)) {
        value = muleContext.getExpressionManager().evaluate(paramValue, event, flowConstruct).getValue();
      } else {
        value = valueOf(paramValue);
      }

      resolvedParameters.put(entry.getKey(), value);
    }

    Optional<ProcessorInterceptorCallback> interceptorCallbackOptional = muleContext.getProcessorInterceptorManager()
        .retrieveInterceptorCallback(componentIdentifier);
    if (interceptorCallbackOptional.isPresent()) {
      ProcessorInterceptorCallback processorInterceptorCallback = interceptorCallbackOptional.get();
      try {
        processorInterceptorCallback.before(event, resolvedParameters);
        Event resultEvent;
        if (processorInterceptorCallback.shouldInterceptExecution(event, resolvedParameters)) {
          resultEvent = processorInterceptorCallback.getResult(event);
        } else {
          resultEvent = intercepted.process(event);
        }
        processorInterceptorCallback.after(resultEvent, resolvedParameters);
        if (next != null) {
          return next.process(resultEvent);
        }
        return resultEvent;
      } catch (MuleException e) {
        throw e;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }
    return intercepted.process(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).handle(nullSafeMap(checkedFunction(event -> process(event))));
  }

}
