/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.policy.AroundPolicy;
import org.mule.api.processor.policy.Policies;
import org.mule.api.processor.policy.PolicyInvocation;
import org.mule.endpoint.EndpointAware;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.util.StringUtils;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if
 * this chain is nested in another chain the next MessageProcessor in the parent chain is not injected into
 * the first in the nested chain.
 */
public abstract class AbstractMessageProcessorChain extends AbstractInterceptingMessageProcessor
                                                    implements MessageProcessorChain, Lifecycle, FlowConstructAware, MuleContextAware, EndpointAware
{
    protected final transient Log log = LogFactory.getLog(getClass());
    protected String name;
    protected List<MessageProcessor> processors;
    private final Policies policies = new Policies(this);

    public AbstractMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        this.name = name;
        this.processors = processors;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (log.isDebugEnabled())
        {
            log.debug(String.format("Invoking %s with event %s", this, event));
        }
        if (event == null)
        {
            return null;
        }

        final List<AroundPolicy> activePolicies = policies.list();
        MuleEvent result;
        if (!activePolicies.isEmpty())
        {
            // if there's a policy, adapt, so it can call through to the doProcess() method
            // TODO I hate to do this, and there are no method delegates in java.
            // This doProcess() must be abstracted into some chain processor which has the logic,
            // and have the chain handle the plumbing only
            PolicyInvocation invocation = new PolicyInvocation(event, activePolicies, new MessageProcessor()
            {
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    return doProcess(event);
                }
            });
            final AroundPolicy entryPoint = activePolicies.get(0);
            result = entryPoint.invoke(invocation);
        }
        else
        {
            // direct invocation
            result = doProcess(event);
        }
        return processNext(result);
    }

    protected abstract MuleEvent doProcess(MuleEvent event) throws MuleException;

    public void initialise() throws InitialisationException
    {
        for (MessageProcessor processor : processors)
        {
            // MULE-5002 TODO review MP Lifecycle
            if (processor instanceof Initialisable /* && !(processor instanceof Transformer) */)
            {
                ((Initialisable) processor).initialise();
            }
        }
    }

    public void start() throws MuleException
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Startable)
            {
                ((Startable) processor).start();
            }
        }
    }

    public void stop() throws MuleException
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Stoppable)
            {
                ((Stoppable) processor).stop();
            }
        }
    }

    public void dispose()
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Disposable)
            {
                ((Disposable) processor).dispose();
            }
        }
        processors.clear();
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof FlowConstructAware)
            {
                ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
            }
        }
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        StringBuilder string = new StringBuilder();
        string.append(getClass().getSimpleName());
        if (StringUtils.isNotBlank(name))
        {
            string.append(String.format(" '%s' ", name));
        }

        Iterator<MessageProcessor> mpIterator = processors.iterator();

        final String nl = String.format("%n");

        for (AroundPolicy policy : policies.list())
        {
            string.append(String.format("%n  -- policy [%s]: %s", policy.getName(), policy));
        }

        // TODO have it print the nested structure with indents increasing for nested MPCs
        if (mpIterator.hasNext())
        {
            string.append(String.format("%n[ "));
            while (mpIterator.hasNext())
            {
                MessageProcessor mp = mpIterator.next();
                final String indented = StringUtils.replace(mp.toString(), nl, String.format("%n  "));
                string.append(String.format("%n  %s", indented));
                if (mpIterator.hasNext())
                {
                    string.append(", ");
                }
            }
            string.append(String.format("%n]"));
        }

        return string.toString();
    }

    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof EndpointAware)
            {
                 ((EndpointAware) processor).setEndpoint(endpoint);
            }
        }
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return processors;
    }

    public Policies getPolicies()
    {
        return policies;
    }
}
