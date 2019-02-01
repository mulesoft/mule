/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.AbstractSplitter;
import org.mule.routing.CorrelationMode;
import org.mule.routing.DefaultRouterResultsHandler;
import org.mule.routing.MessageSequence;
import org.mule.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.Transformer;

/**
 * Base implementation of a {@link MuleMessage} splitter, that converts its payload in a {@link MessageSequence}, and
 * process each element of it. Implementations must implement {@link #splitMessageIntoSequence(MuleEvent)} and determine
 * how the message is split.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www
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
    public MuleEvent process(MuleEvent event) throws MuleException
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

    protected boolean isSequential() { return false; }

    /**
     * Converts the event into a {@link MessageSequence} that will retrieve each of the event elements
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
        String correlationId = messageInfoMapping.getCorrelationId(originalEvent.getMessage());
        List<MuleEvent> resultEvents = new ArrayList<MuleEvent>();
        int correlationSequence = 0;
        MessageSequence<?> messageSequence = seq;
        if (batchSize > 1)
        {
            messageSequence = new PartitionedMessageSequence(seq, batchSize);
        }
        int count = messageSequence.size();
        MuleEvent currentEvent = originalEvent;
        for (; messageSequence.hasNext();)
        {
            Object payload = messageSequence.next();
            if (payload instanceof Collection)
            {
                payload = CollectionUtils.collect((Collection) payload, new Transformer()
                {
                    @Override
                    public Object transform(Object input)
                    {
                        if (input instanceof MuleMessage)
                        {
                            return ((MuleMessage) input).getPayload();
                        }
                        else
                        {
                            return input;
                        }
                    }
                });
            }

            MuleMessage message = createMessage(payload, originalEvent.getMessage());
            correlationSequence++;
            if (counterVariableName != null)
            {
                message.setInvocationProperty(counterVariableName, correlationSequence);
            }
            if (enableCorrelation != CorrelationMode.NEVER)
            {
                boolean correlationSet = message.getCorrelationId() != null;
                if ((!correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
                    || (enableCorrelation == CorrelationMode.ALWAYS))
                {
                    message.setCorrelationId(correlationId + (this.isSequential() ? ("-" + correlationSequence) : ""));
                }

                // take correlation group size from the message properties, set by
                // concrete
                // message splitter implementations
                message.setCorrelationGroupSize(count);
                message.setCorrelationSequence(correlationSequence);
            }
            message.propagateRootId(originalEvent.getMessage());
            MuleEvent resultEvent = processNext(RequestContext.setEvent(new DefaultMuleEvent(message, originalEvent, currentEvent.getSession())));
            if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent))
            {
                currentEvent = resultEvent;
                resultEvents.add(resultEvent);
            }
        }
        if (correlationSequence == 1)
        {
            logger.debug("Splitter only returned a single result. If this is not expected, please check your split expression");
        }
        return resultEvents;
    }

    private MuleMessage createMessage(Object payload, MuleMessage originalMessage)
    {
        if (payload instanceof MuleMessage)
        {
            return (MuleMessage) payload;
        }
        MuleMessage message = new DefaultMuleMessage(originalMessage, muleContext);
        message.setPayload(payload);
        return message;
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
