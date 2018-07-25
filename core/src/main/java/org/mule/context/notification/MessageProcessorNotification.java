/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.NameableObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.MessageProcessorPathResolver;
import org.mule.api.context.notification.AbstractBlockingServerEvent;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.processor.MessageProcessor;
import org.mule.transport.NullPayload;
import org.mule.util.ObjectUtils;

public class MessageProcessorNotification extends ServerNotification implements BlockingServerEvent
{

    private static final long serialVersionUID = 1L;

    public static final int MESSAGE_PROCESSOR_PRE_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 1;

    public static final int MESSAGE_PROCESSOR_POST_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 2;

    /**
     * This action should be only used when you need to get the original event and not a copy of it.
     * In this case, the <code>MuleEvent<code/> must be used in read-only mode to avoid thread access errors.
     */
    public static final int MESSAGE_PROCESSOR_PRE_INVOKE_ORIGINAL_EVENT = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 3;


    private final transient MessageProcessor processor;

    static
    {
        registerAction("message processor pre invoke", MESSAGE_PROCESSOR_PRE_INVOKE);
        registerAction("message processor pre invoke that provides original event", MESSAGE_PROCESSOR_PRE_INVOKE_ORIGINAL_EVENT);
        registerAction("message processor post invoke", MESSAGE_PROCESSOR_POST_INVOKE);
    }

    private static ThreadLocal<String> lastRootMessageId = new ThreadLocal<String>();
    private MessagingException exceptionThrown;

    public MessageProcessorNotification(FlowConstruct flowConstruct,
                                        MuleEvent event,
                                        MessageProcessor processor,
                                        MessagingException exceptionThrown, int action)
    {
        super(produceEvent(event, flowConstruct), action, flowConstruct != null ? flowConstruct.getName() : null, action == MESSAGE_PROCESSOR_PRE_INVOKE_ORIGINAL_EVENT);
        this.exceptionThrown = exceptionThrown;
        this.processor = processor;
    }

    @Override
    public MuleEvent getSource()
    {
        if (source instanceof String)
        {
            return null;
        }
        return (MuleEvent) super.getSource();
    }

    public MessageProcessor getProcessor()
    {
        return processor;
    }

    @Override
    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", processor=" + processorToString() + ", resourceId=" + resourceIdentifier
               + ", serverId=" + getServerId() + ", timestamp=" + timestamp + "}";
    }

    protected String processorToString()
    {
        if (processor == null)
        {
            return "";
        }

        String name;
        if (processor instanceof NameableObject)
        {
            name = String.format("%s '%s'", processor.getClass().getName(), ((NameableObject) processor).getName());
        }
        else
        {
            name = ObjectUtils.identityToString(processor);
        }
        return name;
    }

    /**
     * If event is null, produce and event with the proper message root ID, to allow it to be correlated
     * with others in the thread
     */
    private static MuleEvent produceEvent(MuleEvent sourceEvent, FlowConstruct flowConstruct)
    {
        String rootId = lastRootMessageId.get();
        if (sourceEvent != null && !VoidMuleEvent.getInstance().equals(sourceEvent))
        {
            lastRootMessageId.set(sourceEvent.getMessage().getMessageRootId());
            return sourceEvent;
        }
        else if (rootId != null && flowConstruct != null)
        {
            DefaultMuleMessage msg = new DefaultMuleMessage(NullPayload.getInstance(), flowConstruct.getMuleContext());
            msg.setMessageRootId(rootId);
            return new DefaultMuleEvent(msg, MessageExchangePattern.REQUEST_RESPONSE, flowConstruct);
        }
        else
        {
            return null;
        }
    }

    public MessagingException getExceptionThrown()
    {
        return exceptionThrown;
    }

    public String getProcessorPath()
    {
        FlowConstruct fc = getSource().getFlowConstruct();
        if (!(fc instanceof MessageProcessorPathResolver))
        {
            return null;
        }
        return ((MessageProcessorPathResolver) fc).getProcessorPath(processor);
    }
}
