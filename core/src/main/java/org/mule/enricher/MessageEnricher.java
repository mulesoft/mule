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

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.processor.MessageProcessor;

public class MessageEnricher implements MessageProcessor
{

    private String evaluatorExpression;
    private String enricherExpression;

    private MessageProcessor enrichmentProcessor;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleContext muleContext = event.getMuleContext();
        ExpressionManager expressionManager = muleContext.getExpressionManager();

        Object enrichmentObject = null;
        MuleMessage enrichmentMessage = enrichmentProcessor.process(event).getMessage();

        if (enrichmentMessage != null)
        {
            if (evaluatorExpression != null)
            {
                enrichmentObject = expressionManager.evaluate(evaluatorExpression, enrichmentMessage);
                if (enrichmentObject instanceof MuleMessage)
                {
                    enrichmentObject = ((MuleMessage) enrichmentObject).getPayload();
                }
            }
            else
            {
                enrichmentObject = enrichmentMessage.getPayload();
            }
            expressionManager.enrich(enricherExpression, event.getMessage(), enrichmentObject);
        }
        return event;
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

    public void setEvaluatorExpression(String evaluatorExpression)
    {
        this.evaluatorExpression = evaluatorExpression;
    }

    public void setEnricherExpression(String enricherExpression)
    {
        this.enricherExpression = enricherExpression;
    }

}
