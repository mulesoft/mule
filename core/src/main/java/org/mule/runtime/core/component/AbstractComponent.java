/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.component;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.api.util.StreamingUtils.withCursoredEvent;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.context.notification.ComponentMessageNotification;
import org.mule.runtime.core.internal.context.notification.OptimisedNotificationHandler;
import org.mule.runtime.core.internal.VoidResult;
import org.mule.runtime.core.api.management.stats.ComponentStatistics;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.transformer.TransformerTemplate;
import org.mule.runtime.core.api.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract {@link Component} to be used by all {@link Component} implementations.
 */
public abstract class AbstractComponent extends AbstractAnnotatedObject
    implements Component, MuleContextAware, Lifecycle, MessagingExceptionHandlerAware {

  /**
   * logger used by this class
   */
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected FlowConstruct flowConstruct;
  protected ComponentStatistics statistics = null;
  protected ServerNotificationHandler notificationHandler;
  protected List<Interceptor> interceptors = new ArrayList<>();
  protected MessageProcessorChain interceptorChain;
  protected MuleContext muleContext;
  protected ComponentLifecycleManager lifecycleManager;
  private MessagingExceptionHandler messagingExceptionHandler;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public List<Interceptor> getInterceptors() {
    return interceptors;
  }

  public void setInterceptors(List<Interceptor> interceptors) {
    this.interceptors = interceptors;
  }

  public AbstractComponent() {
    statistics = new ComponentStatistics();
    lifecycleManager = new ComponentLifecycleManager(getName(), this);
  }

  private Event invokeInternal(Event event) throws MuleException {
    // Ensure we have event in ThreadLocal
    setCurrentEvent(event);

    if (logger.isTraceEnabled()) {
      logger.trace(String.format("Invoking %s component for service %s", this.getClass().getName(),
                                 flowConstruct.getName()));
    }

    if (!lifecycleManager.getState().isStarted() || lifecycleManager.getState().isStopping()) {
      throw new LifecycleException(CoreMessages.isStopped(flowConstruct.getName()), this);
    }

    // Invoke component implementation and gather statistics
    try {
      fireComponentNotification(event, ComponentMessageNotification.COMPONENT_PRE_INVOKE);

      long startTime = 0;
      if (statistics.isEnabled()) {
        startTime = System.currentTimeMillis();
      }

      Event.Builder resultEventBuilder = Event.builder(event);
      Object result = doInvoke(event, resultEventBuilder);

      if (statistics.isEnabled()) {
        statistics.addExecutionTime(System.currentTimeMillis() - startTime);
      }

      Event resultEvent = createResultEvent(event, resultEventBuilder, result);
      fireComponentNotification(resultEvent,
                                ComponentMessageNotification.COMPONENT_POST_INVOKE);

      return resultEvent;
    } catch (MuleException me) {
      throw me;
    } catch (Exception e) {
      throw new ComponentException(CoreMessages.failedToInvoke(this.toString()), this, e);
    }
  }

  @Override
  public Event process(Event event) throws MuleException {
    return withCursoredEvent(event, cursoredEvent -> {
      if (interceptorChain == null) {
        return invokeInternal(cursoredEvent);
      } else {
        return interceptorChain.process(cursoredEvent);
      }
    });
  }

  protected Event createResultEvent(Event event, Event.Builder resultEventBuilder, Object result)
      throws MuleException {
    if (result instanceof Message) {
      return resultEventBuilder.message((Message) result).build();
    } else if (result instanceof VoidResult) {
      return event;
    } else {
      final TransformerTemplate template = new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(result));
      template.setReturnDataType(DataType.builder(DataType.OBJECT).charset(getDefaultEncoding(muleContext)).build());
      return resultEventBuilder.message(muleContext.getTransformationService()
          .applyTransformers(event.getMessage(), event, singletonList(template))).build();
    }
  }

  protected abstract Object doInvoke(Event event, Event.Builder eventBuilder) throws Exception;

  @Override
  public String toString() {
    return String.format("%s{%s}", ClassUtils.getSimpleName(this.getClass()), getName());
  }

  public void release() {
    // nothing to do
  }

  @Override
  public ComponentStatistics getStatistics() {
    return statistics;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }

  @Override
  public final void initialise() throws InitialisationException {
    if (flowConstruct == null) {
      throw new InitialisationException(
                                        I18nMessageFactory
                                            .createStaticMessage("Component has not been initialized properly, no flow constuct."),
                                        this);
    }

    lifecycleManager.fireInitialisePhase((phaseName, object) -> {
      DefaultMessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder();
      chainBuilder.setName("Component interceptor processor chain for :" + getName());
      for (Interceptor interceptor : interceptors) {
        chainBuilder.chain(interceptor);
      }
      chainBuilder.chain(new AnnotatedProcessor(event -> invokeInternal(event)));
      interceptorChain = chainBuilder.build();
      applyLifecycleAndDependencyInjection(interceptorChain);
      doInitialise();
    });
  }

  protected void applyLifecycleAndDependencyInjection(Object object) throws InitialisationException {
    if (object instanceof MuleContextAware) {
      ((MuleContextAware) object).setMuleContext(muleContext);
    }
    if (object instanceof FlowConstructAware) {
      ((FlowConstructAware) object).setFlowConstruct(flowConstruct);
    }
    if (object instanceof MessagingExceptionHandlerAware) {
      ((MessagingExceptionHandlerAware) object).setMessagingExceptionHandler(messagingExceptionHandler);
    }
    if (object instanceof Initialisable) {
      ((Initialisable) object).initialise();
    }
  }

  protected void doInitialise() throws InitialisationException {
    // Default implementation is no-op
  }

  @Override
  public void dispose() {
    lifecycleManager.fireDisposePhase((phaseName, object) -> doDispose());
  }

  protected void doDispose() {
    // Default implementation is no-op
  }

  @Override
  public void stop() throws MuleException {
    try {
      lifecycleManager.fireStopPhase((phaseName, object) -> doStop());
    } catch (MuleException e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void doStart() throws MuleException {
    // Default implementation is no-op
  }

  @Override
  public void start() throws MuleException {
    lifecycleManager.fireStartPhase((phaseName, object) -> {
      notificationHandler = new OptimisedNotificationHandler(muleContext.getNotificationManager(),
                                                             ComponentMessageNotification.class);
      doStart();
    });

  }

  protected void doStop() throws MuleException {
    // Default implementation is no-op
  }

  protected void fireComponentNotification(Event event, int action) {
    if (notificationHandler != null
        && notificationHandler.isNotificationEnabled(ComponentMessageNotification.class)) {
      notificationHandler
          .fireNotification(new ComponentMessageNotification(createInfo(event, null, this),
                                                             flowConstruct, action));
    }
  }

  protected String getName() {
    StringBuilder sb = new StringBuilder();
    if (flowConstruct != null) {
      sb.append(flowConstruct.getName());
      sb.append(".");
    }
    sb.append("component");
    sb.append(".");
    sb.append(System.identityHashCode(this));
    return sb.toString();
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
  }

  /**
   * Annotated processor only meant to be used internal for the creation of the processors inside the component chain.
   */
  public static class AnnotatedProcessor extends AbstractAnnotatedObject implements Processor {

    private Processor processor;

    public AnnotatedProcessor(Processor processor) {
      this.processor = processor;
    }

    @Override
    public Event process(Event event) throws MuleException {
      return processor.process(event);
    }
  }
}
