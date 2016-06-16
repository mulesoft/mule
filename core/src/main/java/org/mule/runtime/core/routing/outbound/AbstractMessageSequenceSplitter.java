/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.routing.MessageInfoMapping;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.routing.AbstractSplitter;
import org.mule.runtime.core.routing.CorrelationMode;
import org.mule.runtime.core.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.routing.MessageSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation of a {@link MuleMessage} splitter, that converts its payload 
 * in a {@link MessageSequence}, and process each element of it.     
 * Implementations must implement {@link #splitMessageIntoSequence(MuleEvent)} and determine how 
 * the message is split.
 * <p>
 * <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/Sequencer.html">http://www
 * .eaipatterns.com/Sequencer.html</a>
 * 
 * @author flbulgarelli
 * @see AbstractSplitter
 */
public abstract class AbstractMessageSequenceSplitter extends AbstractInterceptingMessageProcessor
    implements MuleContextAware
{
    protected MuleContext muleContext;
    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
    protected CorrelationMode enableCorrelation = CorrelationMode.IF_NOT_SET;
    protected MessageInfoMapping messageInfoMapping;
    protected int batchSize;
    protected String counterVariableName;

    @Override
    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        if (isSplitRequired(event))
        {
            MessageSequence<?> seq = splitMessageIntoSequence(event);
            if (!seq.isEmpty())
            {
                MuleEvent aggregatedResults = resultsHandler.aggregateResults(processParts(seq, event), event, muleContext);
                if (aggregatedResults instanceof VoidMuleEvent)
                {
                    return null;
                }
                else
                {
                    return aggregatedResults;
                }
            }
            else
            {
                logger.warn("Splitter returned no results. If this is not expected, please check your split expression");
                return VoidMuleEvent.getInstance();
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

    /**
     * Converts the event into a {@link MessageSequence} that will retrieve each of
     * the event elements
     * 
     * @param event the event to split
     * @return a sequence of elements
     * @throws MuleException
     */
    protected abstract MessageSequence<?> splitMessageIntoSequence(MuleEvent event) throws MuleException;

    protected List<MuleEvent> processParts(MessageSequence<?> seq, MuleEvent originalEvent) throws MuleException
    {
        if (messageInfoMapping == null)
        {
            messageInfoMapping = originalEvent.getFlowConstruct().getMessageInfoMapping();
        }
        String correlationId = messageInfoMapping.getCorrelationId(originalEvent);
        List<MuleEvent> resultEvents = new ArrayList<MuleEvent>();
        int correlationSequence = 0;
        MessageSequence<?> messageSequence = seq;
        if (batchSize > 1)
        {
            messageSequence = new PartitionedMessageSequence(seq, batchSize);
        }
        int count = messageSequence.size();
        for (; messageSequence.hasNext();)
        {
            MuleEvent event = createEvent(messageSequence.next(), originalEvent);

            correlationSequence++;
            if (counterVariableName != null)
            {
                originalEvent.setFlowVariable(counterVariableName, correlationSequence);
            }

            int finalCorrelationSequence = correlationSequence;
            event.setMessage(event.getMessage().transform(msg -> {
                if (enableCorrelation != CorrelationMode.NEVER)
                {
                    boolean correlationSet = event.getMessage().getCorrelationId() != null;
                    if ((!correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
                        || (enableCorrelation == CorrelationMode.ALWAYS))
                    {
                        msg.setCorrelationId(correlationId);
                    }

                    // take correlation group size from the message properties, set by
                    // concrete
                    // message splitter implementations
                    msg.setCorrelationGroupSize(count);
                    msg.setCorrelationSequence(finalCorrelationSequence);
                }
                msg.propagateRootId(originalEvent.getMessage());
                return msg;
            }));

            MuleEvent resultEvent = processNext(RequestContext.setEvent(event));
            if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent))
            {
                resultEvents.add(resultEvent);
            }
        }
        if (correlationSequence == 1)
        {
            logger.debug("Splitter only returned a single result. If this is not expected, please check your split expression");
        }
        return resultEvents;
    }

    private MuleEvent createEvent(Object payload, MuleEvent originalEvent)
    {
        if (payload instanceof MuleEvent)
        {
            return new DefaultMuleEvent(((MuleEvent) payload).getMessage(), originalEvent);
        }
        else if (payload instanceof MuleMessage)
        {
            return new DefaultMuleEvent((MuleMessage) payload, originalEvent);
        }
        else
        {
            MuleMessage message = new DefaultMuleMessage(payload, originalEvent.getMessage(), muleContext);
            return new DefaultMuleEvent(message, originalEvent);
        }
    }

    public void setEnableCorrelation(CorrelationMode enableCorrelation)
    {
        this.enableCorrelation = enableCorrelation;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void setMessageInfoMapping(MessageInfoMapping messageInfoMapping)
    {
        this.messageInfoMapping = messageInfoMapping;
    }

    /**
     * Split the elements in groups of the specified size
     */
    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }

    public void setCounterVariableName(String counterVariableName)
    {
        this.counterVariableName = counterVariableName;
    }
}
