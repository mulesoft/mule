/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.DefaultEventContext.child;
import static org.mule.runtime.core.api.Event.builder;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.AnnotatedProcessor;
import org.mule.runtime.core.processor.chain.AbstractMessageProcessorChain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class FlowRefFactoryBean extends AbstractAnnotatedObject
    implements FactoryBean<Processor>, ApplicationContextAware, MuleContextAware, Initialisable, Disposable {

  private abstract class FlowRefMessageProcessor
      implements AnnotatedProcessor, FlowConstructAware {

    protected FlowConstruct flowConstruct;

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

  }

  private abstract class FlowRefMessageProcessorContainer extends FlowRefMessageProcessor
      implements FlowConstructAware {

    private Processor dynamicMessageProcessor;

    protected void setResolvedMessageProcessor(Processor dynamicMessageProcessor) {
      this.dynamicMessageProcessor = dynamicMessageProcessor;
    }

    @Override
    public Object getAnnotation(QName name) {
      return FlowRefFactoryBean.this.getAnnotation(name);
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return FlowRefFactoryBean.this.getAnnotations();
    }
  }

  private static final String NULL_FLOW_CONTRUCT_NAME = "null";
  private static final String MULE_PREFIX = "_mule-";
  private String refName;
  private ApplicationContext applicationContext;
  private MuleContext muleContext;
  private Processor referencedMessageProcessor;
  private ConcurrentMap<String, Processor> referenceCache = new ConcurrentHashMap<>();
  private boolean wasInitialized = false;

  public void setName(String name) {
    this.refName = name;
  }

  @Override
  public void initialise() throws InitialisationException {
    // TODO remove this validation (and attribute) after MULE-12096 is fixed
    if (wasInitialized) {
      return;
    }
    if (refName.isEmpty()) {
      throw new InitialisationException(CoreMessages.objectIsNull("flow reference is empty"), this);
    } else if (!muleContext.getExpressionManager().isExpression(refName)) {
      // No need to initialize because message processor will be injected into and managed by parent
      referencedMessageProcessor = lookupReferencedFlowInApplicationContext(refName);
    }
    wasInitialized = true;
  }

  @Override
  public void dispose() {
    for (Processor processor : referenceCache.values()) {
      if (processor instanceof Disposable) {
        ((Disposable) processor).dispose();
      }
    }
    referenceCache = null;
  }

  @Override
  public Processor getObject() throws Exception {
    // TODO remove the call to initialise after MULE-12096 is fixed
    initialise();
    Processor processor =
        referencedMessageProcessor != null ? referencedMessageProcessor : createDynamicReferenceMessageProcessor(refName);
    // Wrap in chain to ensure the flow-ref element always has a path element and lifecycle will be propgated to child sub-flows
    return new AbstractMessageProcessorChain(singletonList(processor)) {};
  }

  protected Processor createDynamicReferenceMessageProcessor(String name) throws MuleException {
    if (name == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
    } else if (!referenceCache.containsKey(name)) {
      Processor dynamicReference = new FlowRefMessageProcessorContainer() {

        @Override
        public Event process(Event event) throws MuleException {
          try {
            final Processor dynamicMessageProcessor = resolveReferencedProcessor(event);
            // Because this is created dynamically annotations cannot be injected by Spring and so
            // FlowRefMessageProcessor is not used here.
            return createParentEvent(event, ((Processor) event1 -> dynamicMessageProcessor.process(event1))
                .process(createChildEvent(event)));
          } catch (MessagingException me) {
            me.setProcessedEvent(createParentEvent(event, me.getEvent()));
            throw me;
          }
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
            // If referenced processor is a Flow used a child EventContext and wait for completion else simply compose.
            if (referencedProcessor instanceof Flow) {
              Event childEvent = createChildEvent(event);
              just(childEvent)
                  .transform(referencedProcessor)
                  // Use empty error handler to avoid reactor ErrorCallbackNotImplemented
                  .subscribe(null, throwable -> {
                  });
              return from(childEvent.getContext().getResponsePublisher())
                  .map(result -> builder(event.getContext(), result).build())
                  .doOnError(MessagingException.class,
                             me -> me.setProcessedEvent(createParentEvent(event, me.getEvent())));
            } else {
              return just(event).transform(referencedProcessor);
            }
          });
        }

        private Processor resolveReferencedProcessor(Event event) throws MuleException {
          // Need to initialize because message processor won't be managed by parent
          String flowName = muleContext.getExpressionManager().parse(refName, event, flowConstruct);
          final Processor dynamicMessageProcessor = getReferencedFlow(flowName, flowConstruct);
          setResolvedMessageProcessor(dynamicMessageProcessor);
          return dynamicMessageProcessor;
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
        public ComponentLocation getLocation() {
          return FlowRefFactoryBean.this.getLocation();
        }
      };
      if (dynamicReference instanceof Initialisable) {
        ((Initialisable) dynamicReference).initialise();
      }
      referenceCache.putIfAbsent(name, dynamicReference);
    }
    return referenceCache.get(name);
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
    String categorizedName = getReferencedFlowCategorizedName(name, flowConstruct);
    if (!referenceCache.containsKey(categorizedName)) {
      Processor referencedFlow = lookupReferencedFlowInApplicationContext(name);
      if (referencedFlow instanceof Initialisable) {
        if (referencedFlow instanceof FlowConstructAware) {
          ((FlowConstructAware) referencedFlow).setFlowConstruct(flowConstruct);
        }
        if (referencedFlow instanceof MuleContextAware) {
          ((MuleContextAware) referencedFlow).setMuleContext(muleContext);
        }
        if (referencedFlow instanceof MessageProcessorChain) {
          for (Processor processor : ((MessageProcessorChain) referencedFlow).getMessageProcessors()) {
            if (processor instanceof FlowConstructAware) {
              ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
            }
            if (processor instanceof MuleContextAware) {
              ((MuleContextAware) processor).setMuleContext(muleContext);
            }
          }
        }
        ((Initialisable) referencedFlow).initialise();
      }
      if (referencedFlow instanceof Startable) {
        ((Startable) referencedFlow).start();
      }
      referenceCache.putIfAbsent(categorizedName, referencedFlow);
    }
    return referenceCache.get(categorizedName);
  }

  private String getReferencedFlowCategorizedName(String referencedFlowName, FlowConstruct flowConstruct) {
    String flowConstructName = flowConstruct != null ? flowConstruct.getName() : NULL_FLOW_CONTRUCT_NAME;
    return MULE_PREFIX + flowConstructName + "-" + referencedFlowName;
  }

  protected Processor lookupReferencedFlowInApplicationContext(String name) {
    final Processor referencedFlow = ((Processor) applicationContext.getBean(name));
    if (referencedFlow == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
    }
    if (referencedFlow instanceof Flow) {
      return new FlowRefMessageProcessor() {

        @Override
        public ComponentLocation getLocation() {
          return FlowRefFactoryBean.this.getLocation();
        }

        @Override
        public Event process(Event event) throws MuleException {
          try {
            return createParentEvent(event, referencedFlow.process(createChildEvent(event)));
          } catch (MessagingException me) {
            me.setProcessedEvent(createParentEvent(event, me.getEvent()));
            throw me;
          }
        }

        @Override
        public Publisher<Event> apply(Publisher<Event> publisher) {
          return from(publisher).flatMap(event -> {
            Event childEvent = createChildEvent(event);
            just(childEvent)
                .transform(referencedFlow)
                // Use empty error handler to avoid reactor ErrorCallbackNotImplemented
                .subscribe(null, throwable -> {
                });
            return from(childEvent.getContext().getResponsePublisher())
                .map(result -> createParentEvent(event, result))
                .doOnError(MessagingException.class,
                           me -> me.setProcessedEvent(createParentEvent(event, me.getEvent())));
          });
        }
      };
    } else {
      if (referencedFlow instanceof AnnotatedObject) {
        ((AnnotatedObject) referencedFlow).setAnnotations(getAnnotations());
      }
      return referencedFlow;
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
}
