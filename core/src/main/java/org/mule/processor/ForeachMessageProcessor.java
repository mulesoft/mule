/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.expression.ExpressionConfig;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.CollectionSplitter;
import org.mule.routing.ExpressionSplitter;
import org.mule.routing.outbound.AbstractMessageSequenceSplitter;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <code>Foreach</code> MessageProcessor allows iterating over a collection
 * payload, or any collection obtained by an expression, generating a message for
 * each element.
 * <p>
 * The number of the message being processed is stored in
 * <code>#[variable:counter]</code> and the root message is store in
 * <code>#[variable:rootMessage]</code>. Both variables may be renamed by means of
 * {@link #setCounterVariableName(String)} and
 * {@link #setRootMessageVariableName(String)}.
 * <p>
 * Defining a groupSize greater than one, allows iterating over collections of
 * elements of the specified size.
 * <p>
 * The {@link MuleEvent} sent to the next message processor is the same that arrived
 * to foreach.
 */
public class ForeachMessageProcessor extends AbstractMessageProcessorOwner implements Initialisable, InterceptingMessageProcessor
{

    public static final String ROOT_MESSAGE_PROPERTY = "rootMessage";
    public static final String COUNTER_PROPERTY = "counter";

    protected Log logger = LogFactory.getLog(getClass());

    private List<MessageProcessor> messageProcessors;
    private MessageProcessor ownedMessageProcessor;
    private AbstractMessageSequenceSplitter splitter;
    private MessageProcessor next;
    private String expression;
    private int groupSize;
    private String rootMessageVariableName;
    private String counterVariableName;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        String parentMessageProp = rootMessageVariableName != null ? rootMessageVariableName : ROOT_MESSAGE_PROPERTY;
        event.getMessage().setInvocationProperty(parentMessageProp, event.getMessage());
        ownedMessageProcessor.process(event);
        return processNext(event);
    }

    protected MuleEvent processNext(MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else
        {
            return next.process(event);
        }
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return messageProcessors;
    }

    @Override
    public void setListener(MessageProcessor listener)
    {
        next = listener;
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors) throws MuleException
    {
        this.messageProcessors = messageProcessors;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (expression != null)
        {
            ExpressionConfig config = new ExpressionConfig();
            config.setExpression(expression);
            splitter = new ExpressionSplitter(config);
        }
        else
        {
            splitter = new CollectionSplitter();
        }
        splitter.setGroupSize(groupSize);
        splitter.setCounterVariableName(counterVariableName);
        splitter.setMuleContext(muleContext);
        messageProcessors.add(0, splitter);

        try
        {
            this.ownedMessageProcessor = new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
        super.initialise();
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public void setGroupSize(int groupSize)
    {
        this.groupSize = groupSize;
    }

    public void setRootMessageVariableName(String rootMessageVariableName)
    {
        this.rootMessageVariableName = rootMessageVariableName;
    }

    public void setCounterVariableName(String counterVariableName)
    {
        this.counterVariableName = counterVariableName;
    }

}


