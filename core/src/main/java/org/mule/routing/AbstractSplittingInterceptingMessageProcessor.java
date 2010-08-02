/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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
import org.mule.api.routing.RouterResultsHandler;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.outbound.DefaultRouterResultsHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a message invoking the next message processor one for each split part. Implementations most
 * implement {@link #splitMessage(MuleEvent)} and determine how the message is split.
 * <p>
 * <b>EIP Reference:</b> {@link http://www.eaipatterns.com/Sequencer.html}
 */

public abstract class AbstractSplittingInterceptingMessageProcessor extends
    AbstractInterceptingMessageProcessor implements MuleContextAware
{

    protected MuleContext muleContext;
    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
    protected CorrelationMode enableCorrelation = CorrelationMode.IF_NOT_SET;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        Object result = splitMessage(event);
        if (result instanceof List)
        {
            return resultsHandler.aggregateResults(processParts((List) result, event), event, muleContext);
        }
        else
        {
            logger.warn("Splitter only returned a single result. If this is not expected, please check your split expression");
            MuleEvent resultEvent;
            if (result instanceof MuleMessage)
            {
                resultEvent = new DefaultMuleEvent((MuleMessage) result, event);
            }
            else
            {
                resultEvent = new DefaultMuleEvent(new DefaultMuleMessage(result, muleContext), event);
            }
            return processNext(event);
        }
    }

    protected abstract Object splitMessage(MuleEvent event);

    protected List<MuleEvent> processParts(List parts, MuleEvent event) throws MuleException
    {
        String correlationId = event.getFlowConstruct().getMessageInfoMapping().getCorrelationId(
            event.getMessage());
        List<MuleEvent> resultEvents = new ArrayList<MuleEvent>();
        int correlationSequence = 1;

        for (Object part : parts)
        {
            MuleMessage message;
            if (part instanceof MuleMessage)
            {
                message = (MuleMessage) part;
            }
            else
            {
                message = new DefaultMuleMessage(part, muleContext);
            }

            if (enableCorrelation != CorrelationMode.NEVER)
            {
                boolean correlationSet = message.getCorrelationId() != null;
                if ((!correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
                    || (enableCorrelation == CorrelationMode.ALWAYS))
                {
                    message.setCorrelationId(correlationId);
                }

                // take correlation group size from the message properties, set by concrete
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

}
