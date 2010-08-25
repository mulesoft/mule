package org.mule.processor.builder;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.StringUtils;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in
 * the chain. This is so that if this chain is nested in another chain the next
 * MessageProcessor in the parent chain is not injected into the first in the
 * nested chain.
 */
public class InterceptingChainCompositeMessageProcessor implements MessageProcessor, Lifecycle, FlowConstructAware, MuleContextAware
{
    private Log log;
    private String name;
    private MessageProcessor firstInChain;
    private List<MessageProcessor> allProcessors;

    public InterceptingChainCompositeMessageProcessor(InterceptingMessageProcessor firstInChain,
                                                      List<MessageProcessor> allProcessors,
                                                      String name)
    {
        this.name = name;
        this.firstInChain = firstInChain;
        this.allProcessors = allProcessors;
        // TODO You a custom categories?
        log = LogFactory.getLog(InterceptingChainCompositeMessageProcessor.class);
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Invoking " + this + " with event " + event);
        }
        return firstInChain.process(event);
    }

    public void initialise() throws InitialisationException
    {
        for (MessageProcessor processor : allProcessors)
        {
            //MULE-5002 TODO review MP Lifecycle
            if (processor instanceof Initialisable /*&& !(processor instanceof Transformer)*/)
            {
                ((Initialisable) processor).initialise();
            }
        }
    }

    public void start() throws MuleException
    {
        for (MessageProcessor processor : allProcessors)
        {
            if (processor instanceof Startable)
            {
                ((Startable) processor).start();
            }
        }
    }

    public void stop() throws MuleException
    {
        for (MessageProcessor processor : allProcessors)
        {
            if (processor instanceof Stoppable)
            {
                ((Stoppable) processor).stop();
            }
        }
    }

    public void dispose()
    {
        for (MessageProcessor processor : allProcessors)
        {
            if (processor instanceof Disposable)
            {
                ((Disposable) processor).dispose();
            }
        }
        firstInChain = null;
        allProcessors.clear();
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        for (MessageProcessor processor : allProcessors)
        {
            if (processor instanceof FlowConstructAware)
            {
                ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
            }
        }
    }

    public void setMuleContext(MuleContext context)
    {
        for (MessageProcessor processor : allProcessors)
        {
            if (processor instanceof MuleContextAware)
            {
                ((MuleContextAware) processor).setMuleContext(context);
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuffer string = new StringBuffer();
        string.append("InterceptingChainCompositeMessageProcessor ");
        if (name != null)
        {
            string.append(" '" + name + "' ");
        }

        Iterator<MessageProcessor> mpIterator = allProcessors.iterator();
        if (mpIterator.hasNext())
        {
            string.append("\n[ ");
            while (mpIterator.hasNext())
            {
                MessageProcessor mp = mpIterator.next();
                string.append("\n  " + StringUtils.replace(mp.toString(), "\n", "\n  "));
                if (mpIterator.hasNext())
                {
                    string.append(", ");
                }
            }
            string.append("\n]");
        }

        return string.toString();
    }
}
