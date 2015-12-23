/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.enricher;

import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.processor.InternalMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.api.processor.MessageProcessors;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.processor.AbstractRequestResponseMessageProcessor;
import org.mule.processor.NonBlockingMessageProcessor;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.transformer.types.TypedValue;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The <code>Message Enricher</code> allows the current message to be augmented using data from a seperate
 * resource.
 * <p/>
 * The Mule implementation the <i>Enrichment Resource</i> can be any Message Processor. This allows you to not
 * only use a JDBC endpoint directly but also call out to a remote service via HTTP or even reference another
 * flow or sub-flow.
 * <p/>
 * The Message Processor that implements the <i>Enrichment Resource</i> is invoked with a copy of the current
 * message along with any flow or session variables that are present. Invocation of the this message processor
 * is done in a separate context to the main flow such that any modification to the message (and it's
 * properties and attachments) or flow or session variables will not be reflected in the flow where the
 * enricher is configured.
 * <p/>
 * The <i>Enrichment Resource</i> should always return a result. If it doesn't then the Enricher will simply
 * leave the message untouched.
 * <p/>
 * The way in which the message is enriched (or modified) is by explicitly configuring mappings (source ->
 * target) between the result from the Enrichment Resource and the message using of Mule Expressions. Mule
 * Expressions are used to both select the value to be extracted from result that comes back from the
 * enrichment resource (source) and to define where this value to be inserted into the message (target). The
 * default 'source' if it's not configured is the payload of the result from the enrichment resource..
 * <p/>
 * <b>EIP Reference:</b> <a
 * href="http://eaipatterns.com/DataEnricher.html">http://eaipatterns.com/DataEnricher.html<a/>
 */
public class MessageEnricher extends AbstractMessageProcessorOwner implements NonBlockingMessageProcessor
{

    private List<EnrichExpressionPair> enrichExpressionPairs = new ArrayList<EnrichExpressionPair>();

    private MessageProcessor enrichmentProcessor;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return new EnricherProcessor(enrichmentProcessor, muleContext).process(event);
    }

    protected void enrich(MuleMessage currentMessage,
                          MuleMessage enrichmentMessage,
                          String sourceExpressionArg,
                          String targetExpressionArg,
                          ExpressionManager expressionManager)
    {
        if (StringUtils.isEmpty(sourceExpressionArg))
        {
            sourceExpressionArg = "#[payload:]";
        }

        TypedValue typedValue = expressionManager.evaluateTyped(sourceExpressionArg, enrichmentMessage);

        if (typedValue.getValue() instanceof MuleMessage)
        {
            MuleMessage muleMessage = (MuleMessage) typedValue.getValue();
            typedValue = new TypedValue(muleMessage.getPayload(), muleMessage.getDataType());
        }

        if (!StringUtils.isEmpty(targetExpressionArg))
        {
            expressionManager.enrichTyped(targetExpressionArg, currentMessage, typedValue);
        }
        else
        {
            currentMessage.setPayload(typedValue.getValue(), typedValue.getDataType());
        }
    }

    public void setEnrichmentMessageProcessor(MessageProcessor enrichmentProcessor)
    {
        if (!(enrichmentProcessor instanceof MessageProcessorChain))
        {
            this.enrichmentProcessor = MessageProcessors.singletonChain(enrichmentProcessor);
        }
        else
        {
            this.enrichmentProcessor = enrichmentProcessor;
        }
    }

    /**
     * For spring
     */
    public void setMessageProcessor(MessageProcessor enrichmentProcessor)
    {
        setEnrichmentMessageProcessor(enrichmentProcessor);
    }

    public void setEnrichExpressionPairs(List<EnrichExpressionPair> enrichExpressionPairs)
    {
        this.enrichExpressionPairs = enrichExpressionPairs;
    }

    public void addEnrichExpressionPair(EnrichExpressionPair pair)
    {
        this.enrichExpressionPairs.add(pair);
    }

    public static class EnrichExpressionPair
    {

        private String source;
        private String target;

        public EnrichExpressionPair()
        {
            // for spring
        }

        public EnrichExpressionPair(String target)
        {
            this.target = target;
        }

        public EnrichExpressionPair(String source, String target)
        {
            this.source = source;
            this.target = target;
        }

        public String getSource()
        {
            return source;
        }

        public void setSource(String source)
        {
            this.source = source;
        }

        public String getTarget()
        {
            return target;
        }

        public void setTarget(String target)
        {
            this.target = target;
        }
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return Collections.singletonList(enrichmentProcessor);
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        if (enrichmentProcessor instanceof InterceptingChainLifecycleWrapper)
        {
            super.addMessageProcessorPathElements(pathElement);
        }
        else
        {
            ((MessageProcessorContainer) enrichmentProcessor).addMessageProcessorPathElements(pathElement);
        }
    }

    /**
     * Enriches the current event using the result of processing the next message processor (the enrichment processor)
     * and the configured enrichment pairs.
     */
    private class EnricherProcessor extends AbstractRequestResponseMessageProcessor implements InternalMessageProcessor
    {

        private MuleEvent eventToEnrich;

        protected EnricherProcessor(MessageProcessor enrichmentProcessor, MuleContext muleContext)
        {
            this.next = enrichmentProcessor;
            this.muleContext = muleContext;
        }

        @Override
        protected MuleEvent processBlocking(MuleEvent event) throws MuleException
        {
            this.eventToEnrich = event;
            return super.processBlocking(copyEventForEnrichment(event));
        }

        @Override
        protected MuleEvent processNonBlocking(MuleEvent event) throws MuleException
        {
            this.eventToEnrich = event;
            MuleEvent result = processNext(copyEventForEnrichment(new DefaultMuleEvent(event, createReplyToHandler(event))));
            if (!(result instanceof NonBlockingVoidMuleEvent))
            {
                result = processResponse(result);
            }
            return result;
        }

        private MuleEvent copyEventForEnrichment(MuleEvent event)
        {
            return OptimizedRequestContext.unsafeSetEvent(DefaultMuleEvent.copy(event));
        }

        @Override
        protected MuleEvent processResponse(MuleEvent event) throws MuleException
        {
            // Reset access control on current event instance for continued flow processing
            ((ThreadSafeAccess) eventToEnrich).resetAccessControl();

            final ExpressionManager expressionManager = eventToEnrich.getMuleContext().getExpressionManager();

            if (event != null && !VoidMuleEvent.getInstance().equals(eventToEnrich))
            {
                for (EnrichExpressionPair pair : enrichExpressionPairs)
                {
                    enrich(eventToEnrich.getMessage(), event.getMessage(), pair.getSource(), pair.getTarget(),
                           expressionManager);
                }
            }

            if (muleContext != null
                && muleContext.getConfiguration().isEnricherPropagatesSessionVariableChanges())
            {
                eventToEnrich = new DefaultMuleEvent(eventToEnrich.getMessage(), eventToEnrich, event.getSession());
            }

            return OptimizedRequestContext.unsafeSetEvent(eventToEnrich);
        }

    }
}
