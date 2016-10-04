/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.util.NotificationUtils.buildPathResolver;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.MessageProcessors;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.processor.chain.AbstractMessageProcessorChain;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.processor.chain.DynamicMessageProcessorContainer;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.util.NotificationUtils.FlowMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class FlowRefFactoryBean extends AbstractAnnotatedObject
    implements FactoryBean<Processor>, ApplicationContextAware, MuleContextAware, Initialisable, Disposable {

  private abstract class FlowRefMessageProcessor
      implements NonBlockingMessageProcessor, AnnotatedObject, FlowConstructAware, MessageProcessorContainer {

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
      implements DynamicMessageProcessorContainer {

    private MessageProcessorPathElement pathElement;
    private Processor dynamicMessageProcessor;

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
      this.pathElement = pathElement;
    }

    @Override
    public FlowMap buildInnerPaths() {
      if (dynamicMessageProcessor instanceof MessageProcessorContainer) {
        ((MessageProcessorContainer) dynamicMessageProcessor).addMessageProcessorPathElements(getPathElement());
        return buildPathResolver(getPathElement());
      } else {
        return null;
      }
    }

    public MessageProcessorPathElement getPathElement() {
      return pathElement;
    }

    protected void setResolvedMessageProcessor(Processor dynamicMessageProcessor) {
      this.dynamicMessageProcessor = dynamicMessageProcessor;
    }

  }

  private static final String NULL_FLOW_CONTRUCT_NAME = "null";
  private static final String MULE_PREFIX = "_mule-";
  private String refName;
  private ApplicationContext applicationContext;
  private MuleContext muleContext;
  private Processor referencedMessageProcessor;
  private ConcurrentMap<String, Processor> referenceCache = new ConcurrentHashMap<>();

  public void setName(String name) {
    this.refName = name;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (refName.isEmpty()) {
      throw new InitialisationException(CoreMessages.objectIsNull("flow reference is empty"), this);
    } else if (!muleContext.getExpressionLanguage().isExpression(refName)) {
      // No need to initialize because message processor will be injected into and managed by parent
      referencedMessageProcessor = lookupReferencedFlowInApplicationContext(refName);
    }
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
    Processor processor =
        referencedMessageProcessor != null ? referencedMessageProcessor : createDynamicReferenceMessageProcessor(refName);
    // Wrap in chain to ensure the flow-ref element always has a path element and lifecycle will be propgated to child sub-flows
    return new AbstractMessageProcessorChain(singletonList(processor)) {

      @Override
      public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
        NotificationUtils.addMessageProcessorPathElements(processor, pathElement.addChild(processor));
      }
    };
  }

  protected Processor createDynamicReferenceMessageProcessor(String name) throws MuleException {
    if (name == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
    } else if (!referenceCache.containsKey(name)) {
      Processor dynamicReference = new FlowRefMessageProcessorContainer() {

        @Override
        public Event process(Event event) throws MuleException {
          // Need to initialize because message processor won't be managed by parent
          String flowName = muleContext.getExpressionLanguage().parse(refName, event, flowConstruct);
          final Processor dynamicMessageProcessor = getReferencedFlow(flowName, flowConstruct);
          setResolvedMessageProcessor(dynamicMessageProcessor);

          // Because this is created dynamically annotations cannot be injected by Spring and so
          // FlowRefMessageProcessor is not used here.
          return ((NonBlockingMessageProcessor) event1 -> dynamicMessageProcessor.process(event1)).process(event);
        }
      };
      if (dynamicReference instanceof Initialisable) {
        ((Initialisable) dynamicReference).initialise();
      }
      referenceCache.putIfAbsent(name, dynamicReference);
    }
    return referenceCache.get(name);
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
    if (referencedFlow instanceof FlowConstruct) {
      return new FlowRefMessageProcessor() {

        @Override
        public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
          NotificationUtils.addMessageProcessorPathElements(referencedFlow, pathElement);
        }

        @Override
        public Event process(Event event) throws MuleException {
          return referencedFlow.process(event);
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
    return false;
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
