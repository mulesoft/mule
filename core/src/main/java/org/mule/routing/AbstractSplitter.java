/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.processor.AbstractInterceptingMessageProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a message invoking the next message processor one for each split part.
 * Implementations must implement {@link #splitMessage(MuleEvent)} and determine how
 * the message is split.
 *
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www.eaipatterns.com/Sequencer.html</a>
 */

public abstract class AbstractSplitter extends
    AbstractInterceptingMessageProcessor implements MuleContextAware
{

    protected MuleContext muleContext;
    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
    protected CorrelationMode enableCorrelation = CorrelationMode.IF_NOT_SET;
    protected MessageInfoMapping messageInfoMapping;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (isSplitRequired(event))
        {
            List<MuleMessage> parts = splitMessage(event);
            if (parts.size() > 0)
            {
                if (parts.size() <= 1)
                {
                    logger.warn("Splitter only returned a single result. If this is not expected, please check your split expression");
                }
                return resultsHandler.aggregateResults(processParts(parts, event), event, muleContext);
            }
            else
            {
                logger.warn("Splitter returned no results. If this is not expected, please check your split expression");
                return null;
            }
        }
        else
        {
            return processNext(event);
        }
    }

    protected boolean isSplitRequired(MuleEvent event)
    {
        return true;
    }

    protected abstract List<MuleMessage> splitMessage(MuleEvent event) throws MuleException;

    protected List<MuleEvent> processParts(List parts, MuleEvent event) throws MuleException
    {
        if (messageInfoMapping == null)
        {
            messageInfoMapping = event.getFlowConstruct().getMessageInfoMapping();
        }
        String correlationId = messageInfoMapping.getCorrelationId(
            event.getMessage());
        List<MuleEvent> resultEvents = new ArrayList<MuleEvent>();
        int correlationSequence = 1;

        MuleMessage originalMessage = event.getMessage();
        for (Object part : parts)
        {
            MuleMessage message;
            if (part instanceof MuleMessage)
            {
                message = (MuleMessage) part;
            }
            else
            {               
                message = new DefaultMuleMessage(originalMessage, muleContext);
                message.setPayload(part);
            }

            if (enableCorrelation != CorrelationMode.NEVER)
            {
                boolean correlationSet = message.getCorrelationId() != null;
                if ((!correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
                    || (enableCorrelation == CorrelationMode.ALWAYS))
                {
                    message.setCorrelationId(correlationId);
                }

                // take correlation group size from the message properties, set by
                // concrete
                // message splitter implementations
                message.setCorrelationGroupSize(parts.size());
                message.setCorrelationSequence(correlationSequence++);
            }
            resultEvents.add(processNext(new DefaultMuleEvent(message, event)));
        }
        return resultEvents;
    }

    public void setEnableCorrelation(CorrelationMode enableCorrelation)
    {
        this.enableCorrelation = enableCorrelation;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void setMessageInfoMapping(MessageInfoMapping messageInfoMapping)
    {
        this.messageInfoMapping = messageInfoMapping;
    }

}
