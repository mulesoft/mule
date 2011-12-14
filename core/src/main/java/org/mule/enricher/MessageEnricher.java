/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.enricher;

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.session.DefaultMuleSession;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageEnricher extends AbstractMessageProcessorOwner implements MessageProcessor
{

    private List<EnrichExpressionPair> enrichExpressionPairs = new ArrayList<EnrichExpressionPair>();

    private MessageProcessor enrichmentProcessor;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        ExpressionManager expressionManager = event.getMuleContext().getExpressionManager();
        MuleEvent enrichmentEvent = enrichmentProcessor.process(RequestContext.setEvent(event));

        if (enrichmentEvent != null)
        {
            for (EnrichExpressionPair pair : enrichExpressionPairs)
            {
                enrich(event.getMessage(), enrichmentEvent.getMessage(), pair.getSource(), pair.getTarget(),
                    expressionManager);
            }
            event = new DefaultMuleEvent(event.getMessage(), event, new DefaultMuleSession(
                enrichmentEvent.getSession()));
            event = RequestContext.setEvent(event);
        }
        return event;
    }

    protected void enrich(MuleMessage currentMessage,
                          MuleMessage enrichmentMessage,
                          String sourceExpressionArg,
                          String targetExpressionArg,
                          ExpressionManager expressionManager)
    {
        if (StringUtils.isEmpty(sourceExpressionArg))
        {
            sourceExpressionArg = "#[payload]";
        }

        Object enrichmentObject = expressionManager.evaluate(sourceExpressionArg, enrichmentMessage);
        if (enrichmentObject instanceof MuleMessage)
        {
            enrichmentObject = ((MuleMessage) enrichmentObject).getPayload();
        }

        if (!StringUtils.isEmpty(targetExpressionArg))
        {
            expressionManager.enrich(targetExpressionArg, currentMessage, enrichmentObject);
        }
        else
        {
            currentMessage.setPayload(enrichmentObject);
        }
    }

    public void setEnrichmentMessageProcessor(MessageProcessor enrichmentProcessor)
    {
        this.enrichmentProcessor = enrichmentProcessor;
    }

    /**
     * For spring
     */
    public void setMessageProcessor(MessageProcessor enrichmentProcessor)
    {
        this.enrichmentProcessor = enrichmentProcessor;
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
}
