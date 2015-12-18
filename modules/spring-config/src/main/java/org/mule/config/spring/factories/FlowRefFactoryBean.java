/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import static org.mule.util.NotificationUtils.buildPathResolver;

import org.mule.AbstractAnnotatedObject;
import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.NonBlockingMessageProcessor;
import org.mule.processor.chain.DynamicMessageProcessorContainer;
import org.mule.util.NotificationUtils.FlowMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class FlowRefFactoryBean extends AbstractAnnotatedObject
    implements FactoryBean<MessageProcessor>, ApplicationContextAware, MuleContextAware, Initialisable,
    Disposable
{

    private abstract class FlowRefMessageProcessor implements NonBlockingMessageProcessor, AnnotatedObject
    {
        @Override
        public Object getAnnotation(QName name)
        {
            return FlowRefFactoryBean.this.getAnnotation(name);
        }

        @Override
        public Map<QName, Object> getAnnotations()
        {
            return FlowRefFactoryBean.this.getAnnotations();
        }

        @Override
        public void setAnnotations(Map<QName, Object> annotations)
        {
            FlowRefFactoryBean.this.setAnnotations(annotations);
        }
    }
    
    private abstract class FlowRefMessageProcessorContainer extends FlowRefMessageProcessor implements DynamicMessageProcessorContainer
    {
        private MessageProcessorPathElement pathElement;
        private MessageProcessor dynamicMessageProcessor;

        @Override
        public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
            this.pathElement = pathElement;
        }
        
        @Override
        public FlowMap buildInnerPaths()
        {
            if (dynamicMessageProcessor instanceof MessageProcessorContainer)
            {
                ((MessageProcessorContainer) dynamicMessageProcessor).addMessageProcessorPathElements(getPathElement());
                return buildPathResolver(getPathElement());
            }
            else
            {
                return null;
            }
        }

        public MessageProcessorPathElement getPathElement()
        {
            return pathElement;
        }

        protected void setResolvedMessageProcessor(MessageProcessor dynamicMessageProcessor)
        {
            this.dynamicMessageProcessor = dynamicMessageProcessor;
        }

    }

    private static final String NULL_FLOW_CONTRUCT_NAME = "null";
    private static final String MULE_PREFIX = "_mule-";
    private String refName;
    private ApplicationContext applicationContext;
    private MuleContext muleContext;
    private MessageProcessor referencedMessageProcessor;
    private ConcurrentMap<String, MessageProcessor> referenceCache = new ConcurrentHashMap<String, MessageProcessor>();

    public void setName(String name)
    {
        this.refName = name;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (refName.isEmpty())
        {
            throw new InitialisationException(CoreMessages.objectIsNull("flow reference is empty"), this);
        }
        else if (!muleContext.getExpressionManager().isExpression(refName))
        {
            // No need to initialize because message processor will be injected into and managed by parent
            referencedMessageProcessor = lookupReferencedFlowInApplicationContext(refName);
        }
    }

    @Override
    public void dispose()
    {
        for (MessageProcessor processor : referenceCache.values())
        {
            if (processor instanceof Disposable)
            {
                ((Disposable) processor).dispose();
            }
        }
        referenceCache = null;
    }

    @Override
    public MessageProcessor getObject() throws Exception
    {
        if (referencedMessageProcessor != null)
        {
            return referencedMessageProcessor;
        }
        else
        {
            return createDynamicReferenceMessageProcessor(refName);
        }
    }

    protected MessageProcessor createDynamicReferenceMessageProcessor(String name) throws MuleException
    {
        if (name == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
        }
        else if (!referenceCache.containsKey(name))
        {
            MessageProcessor dynamicReference = new FlowRefMessageProcessorContainer()
            {
                @Override
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    // Need to initialize because message processor won't be managed by parent
                    String flowName = muleContext.getExpressionManager()
                                                 .parse(refName, event);
                    final MessageProcessor dynamicMessageProcessor = getReferencedFlow(flowName, event.getFlowConstruct());
                    setResolvedMessageProcessor(dynamicMessageProcessor);

                    // Because this is created dynamically annotations cannot be injected by Spring and so
                    // FlowRefMessageProcessor is not used here.
                    return new NonBlockingMessageProcessor()
                    {
                        @Override
                        public MuleEvent process(MuleEvent event) throws MuleException
                        {
                            return dynamicMessageProcessor.process(event);
                        }
                    }.process(event);
                }
            };
            if (dynamicReference instanceof Initialisable)
            {
                ((Initialisable) dynamicReference).initialise();
            }
            referenceCache.putIfAbsent(name, dynamicReference);
        }
        return referenceCache.get(name);
    }

    protected MessageProcessor getReferencedFlow(String name, FlowConstruct flowConstruct) throws MuleException
    {
        if (name == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
        }
        String categorizedName = getReferencedFlowCategorizedName(name, flowConstruct);
        if (!referenceCache.containsKey(categorizedName))
        {
            MessageProcessor referencedFlow = lookupReferencedFlowInApplicationContext(name);
            if (referencedFlow instanceof Initialisable)
            {
                if (referencedFlow instanceof FlowConstructAware)
                {
                    ((FlowConstructAware) referencedFlow).setFlowConstruct(flowConstruct);
                }
                if (referencedFlow instanceof MuleContextAware)
                {
                    ((MuleContextAware) referencedFlow).setMuleContext(muleContext);
                }
                if (referencedFlow instanceof MessageProcessorChain)
                {
                    for (MessageProcessor processor : ((MessageProcessorChain) referencedFlow).getMessageProcessors())
                    {
                        if (processor instanceof FlowConstructAware)
                        {
                            ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
                        }
                        if (processor instanceof MuleContextAware)
                        {
                            ((MuleContextAware) processor).setMuleContext(muleContext);
                        }
                    }
                }
                ((Initialisable) referencedFlow).initialise();
            }
            if (referencedFlow instanceof Startable)
            {
                ((Startable) referencedFlow).start();
            }
            referenceCache.putIfAbsent(categorizedName, referencedFlow);
        }
        return referenceCache.get(categorizedName);
    }

    private String getReferencedFlowCategorizedName(String referencedFlowName, FlowConstruct flowConstruct)
    {
        String flowConstructName = flowConstruct != null ? flowConstruct.getName() : NULL_FLOW_CONTRUCT_NAME;
        return MULE_PREFIX + flowConstructName + "-" + referencedFlowName;
    }

    protected MessageProcessor lookupReferencedFlowInApplicationContext(String name)
    {
        final MessageProcessor referencedFlow = ((MessageProcessor) applicationContext.getBean(name));
        if (referencedFlow == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
        }
        if (referencedFlow instanceof FlowConstruct)
        {
            return new FlowRefMessageProcessor()
            {
                @Override
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    return referencedFlow.process(event);
                }
            };
        }
        else
        {
            if (referencedFlow instanceof AnnotatedObject)
            {
                ((AnnotatedObject) referencedFlow).setAnnotations(getAnnotations());
            }
            return referencedFlow;
        }
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public Class<?> getObjectType()
    {
        return MessageProcessor.class;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
