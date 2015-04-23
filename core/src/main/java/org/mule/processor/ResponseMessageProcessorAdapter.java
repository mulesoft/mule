/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.MessageProcessors;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.processor.chain.ProcessorExecutorFactory;

import java.util.Collections;
import java.util.List;

public class ResponseMessageProcessorAdapter extends AbstractRequestResponseMessageProcessor implements Lifecycle,
        FlowConstructAware
{

    protected MessageProcessor responseProcessor;
    protected FlowConstruct flowConstruct;

    public ResponseMessageProcessorAdapter()
    {
        super();
    }

    public ResponseMessageProcessorAdapter(MessageProcessor responseProcessor)
    {
        super();
        this.responseProcessor = responseProcessor;
    }

    public void setProcessor(MessageProcessor processor)
    {
        this.responseProcessor = processor;
    }

    @Override
    protected MuleEvent processResponse(MuleEvent event) throws MuleException
    {
        if (responseProcessor == null || !isEventValid(event))
        {
            return event;
        }
        else
        {
            return new CopyOnNullNonBlockingProcessorExceutor(event, Collections.singletonList(responseProcessor),
                                                              MessageProcessorExecutionTemplate
                                                                      .createExecutionTemplate(), true).execute();
        }
    }

    class CopyOnNullNonBlockingProcessorExceutor extends NonBlockingProcessorExecutor
    {

        public CopyOnNullNonBlockingProcessorExceutor(MuleEvent event, List<MessageProcessor> processors,
                                                      MessageProcessorExecutionTemplate executionTemplate, boolean
                copyOnVoidEvent)
        {
            super(event, processors, executionTemplate, copyOnVoidEvent);
        }

        @Override
        protected boolean isUseEventCopy(MuleEvent result)
        {
            return super.isUseEventCopy(result) || result == null;
        }
    }

    public void initialise() throws InitialisationException
    {
        if (responseProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware) responseProcessor).setMuleContext(muleContext);
        }
        if (responseProcessor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) responseProcessor).setFlowConstruct(flowConstruct);
        }
        if (responseProcessor instanceof Initialisable)
        {
            ((Initialisable) responseProcessor).initialise();
        }
    }

    public void start() throws MuleException
    {
        if (responseProcessor instanceof Startable)
        {
            ((Startable) responseProcessor).start();
        }
    }

    public void stop() throws MuleException
    {
        if (responseProcessor instanceof Stoppable)
        {
            ((Stoppable) responseProcessor).stop();
        }
    }

    public void dispose()
    {
        if (responseProcessor instanceof Disposable)
        {
            ((Disposable) responseProcessor).dispose();
        }
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

}
