/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NonBlockingSupported;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.api.routing.filter.Filter;
import org.mule.api.metadata.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.expression.ExpressionConfig;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.routing.outbound.CollectionMessageSequence;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.NotificationUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * ` * The <code>Foreach</code> MessageProcessor allows iterating over a collection payload, or any collection
 * obtained by an expression, generating a message for each element.
 * <p/>
 * The number of the message being processed is stored in <code>#[variable:counter]</code> and the root
 * message is store in <code>#[variable:rootMessage]</code>. Both variables may be renamed by means of
 * {@link #setCounterVariableName(String)} and {@link #setRootMessageVariableName(String)}.
 * <p/>
 * Defining a groupSize greater than one, allows iterating over collections of elements of the specified size.
 * <p/>
 * The {@link MuleEvent} sent to the next message processor is the same that arrived to foreach.
 */
public class Foreach extends AbstractMessageProcessorOwner implements Initialisable, MessageProcessor, NonBlockingSupported
{

    public static final String ROOT_MESSAGE_PROPERTY = "rootMessage";
    public static final String COUNTER_PROPERTY = "counter";
    private static final String XPATH_PREFIX = "xpath";

    protected Log logger = LogFactory.getLog(getClass());

    private List<MessageProcessor> messageProcessors;
    private List<MessageProcessor> ownedMessageProcessors;
    private MessageProcessor ownedRootMessageProcessor;
    private AbstractMessageSequenceSplitter splitter;
    private String collectionExpression;
    private ExpressionConfig expressionConfig = new ExpressionConfig();
    private int batchSize;
    private String rootMessageVariableName;
    private String counterVariableName;
    private boolean xpathCollection;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        String parentMessageProp = rootMessageVariableName != null
                                   ? rootMessageVariableName
                                   : ROOT_MESSAGE_PROPERTY;
        Object previousCounterVar = null;
        Object previousRootMessageVar = null;
        if (event.getFlowVariableNames().contains(counterVariableName))
        {
            previousCounterVar = event.getFlowVariable(counterVariableName);
        }
        if (event.getFlowVariableNames().contains(parentMessageProp))
        {
            previousRootMessageVar = event.getFlowVariable(parentMessageProp);
        }
        MuleMessage message = event.getMessage();
        boolean transformed = false;
        if (xpathCollection)
        {
            transformed = transformPayloadIfNeeded(message);
        }
        message.setInvocationProperty(parentMessageProp, message);
        ownedRootMessageProcessor.process(event);
        if (transformed)
        {
            transformBack(message);
        }
        if (previousCounterVar != null)
        {
            event.setFlowVariable(counterVariableName, previousCounterVar);
        }
        else
        {
            event.removeFlowVariable(counterVariableName);
        }
        if (previousRootMessageVar != null)
        {
            event.setFlowVariable(parentMessageProp, previousRootMessageVar);
        }
        else
        {
            event.removeFlowVariable(parentMessageProp);
        }
        return event;
    }

    private boolean transformPayloadIfNeeded(MuleMessage message) throws TransformerException
    {
        Object payload = message.getPayload();
        if (payload instanceof Document || payload.getClass().getName().startsWith("org.dom4j."))
        {
            return false;
        }
        message.setPayload(muleContext.getTransformationService().getPayload(message, DataTypeFactory.create(Document.class)));
        return true;
    }

    private void transformBack(MuleMessage message) throws TransformerException
    {
        message.setPayload(muleContext.getTransformationService().getPayload(message, DataType.STRING_DATA_TYPE));
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return ownedMessageProcessors;
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        // skip the splitter that is added at the beginning and the filter at the end
        NotificationUtils.addMessageProcessorPathElements(messageProcessors, pathElement);
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors) throws MuleException
    {
        this.messageProcessors = messageProcessors;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (collectionExpression != null)
        {
            expressionConfig.setExpression(collectionExpression);
            checkEvaluator(expressionConfig);
            splitter = new ExpressionSplitter(expressionConfig);
            if (expressionConfig.getEvaluator() != null && expressionConfig.getEvaluator().startsWith(XPATH_PREFIX))
            {
                xpathCollection = true;
            }
            else if (expressionConfig.getEvaluator() == null && isXPathExpression(expressionConfig.getExpression()))
            {
                xpathCollection = true;
            }
        }
        else
        {
            splitter = new CollectionMapSplitter();
        }
        splitter.setBatchSize(batchSize);
        splitter.setCounterVariableName(counterVariableName);
        splitter.setMuleContext(muleContext);

        ownedMessageProcessors = new ArrayList<>(messageProcessors.size() + 2);
        ownedMessageProcessors.add(splitter);
        ownedMessageProcessors.addAll(messageProcessors);
        ownedMessageProcessors.add(new MessageFilter(new Filter()
        {

            @Override
            public boolean accept(MuleMessage message)
            {
                return false;
            }
        }));

        try
        {
            this.ownedRootMessageProcessor = new DefaultMessageProcessorChainBuilder().chain(ownedMessageProcessors)
                    .build();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
        super.initialise();
    }

    private boolean isXPathExpression(String expression)
    {
        return expression.matches("^xpath\\(.+\\)$") ||
               expression.matches("^xpath3\\(.+\\)$");
    }

    private void checkEvaluator(ExpressionConfig expressionConfig)
    {
        if (expressionConfig.getEvaluator() != null && expressionConfig.getEvaluator().startsWith(XPATH_PREFIX))
        {
            expressionConfig.setEvaluator("xpath-branch");
        }
    }

    public void setCollectionExpression(String expression)
    {
        this.collectionExpression = expression;
    }

    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }

    public void setRootMessageVariableName(String rootMessageVariableName)
    {
        this.rootMessageVariableName = rootMessageVariableName;
    }

    public void setCounterVariableName(String counterVariableName)
    {
        this.counterVariableName = counterVariableName;
    }

    private static class CollectionMapSplitter extends CollectionSplitter
    {

        @Override
        protected MessageSequence<?> splitMessageIntoSequence(MuleEvent event)
        {
            Object payload = event.getMessage().getPayload();
            if (payload instanceof Map<?, ?>)
            {
                List<MuleMessage> list = new LinkedList<MuleMessage>();
                Set<Map.Entry<?, ?>> set = ((Map) payload).entrySet();
                for (Entry<?, ?> entry : set)
                {
                    MuleMessage splitMessage = new DefaultMuleMessage(entry.getValue(), muleContext);
                    splitMessage.setInvocationProperty(MapSplitter.MAP_ENTRY_KEY, entry.getKey());
                    list.add(splitMessage);
                }
                return new CollectionMessageSequence(list);
            }
            return super.splitMessageIntoSequence(event);
        }

    }
}
