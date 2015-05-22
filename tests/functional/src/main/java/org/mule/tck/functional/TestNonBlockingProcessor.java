package org.mule.tck.functional;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.NonBlockingMessageProcessor;
import org.mule.util.ObjectUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *  Test implementation of {@link org.mule.processor.NonBlockingMessageProcessor} that simply uses a @{link Executor} to
 *  invoke the {@link org.mule.api.transport.ReplyToHandler} in another thread.
 */
public class TestNonBlockingProcessor implements NonBlockingMessageProcessor
{

    private static Executor executor = Executors.newCachedThreadPool();

    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        if (!event.getExchangePattern().hasResponse())
        {
            return VoidMuleEvent.getInstance();
        }
        else if (event.isAllowNonBlocking() && event.getReplyToHandler() != null)
        {
            executor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        event.getReplyToHandler().processReplyTo(event, null, null);
                    }
                    catch (MessagingException e)
                    {
                        event.getReplyToHandler().processExceptionReplyTo(e, null);
                    }
                    catch (MuleException e)
                    {
                        event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, e), null);
                    }
                }
            });
            return NonBlockingVoidMuleEvent.getInstance();
        }
        else
        {
            return event;
        }
    }

}
