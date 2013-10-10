/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.ServerNotificationHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.processor.chain.DefaultMessageProcessorChain;
import org.mule.util.ObjectUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract implementation that provides the infrastructure for intercepting message processors.
 * It doesn't implement InterceptingMessageProcessor itself, to let individual subclasses make that decision \.
 * This simply provides an implementation of setNext and holds the next message processor as an
 * attribute.
 */
public abstract class AbstractInterceptingMessageProcessorBase
    implements MessageProcessor, MuleContextAware, AnnotatedObject
{
    protected Log logger = LogFactory.getLog(getClass());

    protected ServerNotificationHandler notificationHandler;

    protected MuleContext muleContext;
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        notificationHandler = muleContext.getNotificationManager();
        if (next instanceof DefaultMessageProcessorChain)
        {
            ((DefaultMessageProcessorChain) next).setMuleContext(context);
        }
    }

    public final MessageProcessor getListener()
    {
        return next;
    }

    public void setListener(MessageProcessor next)
    {
        this.next = next;
    }

    protected MessageProcessor next;

    protected MuleEvent processNext(MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else if (event == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.trace("MuleEvent is null.  Next MessageProcessor '" + next.getClass().getName()
                             + "' will not be invoked.");
            }
            return null;
        }
        else
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
            }

            boolean fireNotification = !(next instanceof MessageProcessorChain) && event.isNotificationsEnabled();

            if (fireNotification)
            {
                // note that we're firing event for the next in chain, not this MP
                fireNotification(event.getFlowConstruct(), event, next,
                    MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);
            }
            final MuleEvent result;
            try
            {
                result = next.process(event);
            }
            catch (MuleException e)
            {
                event.getSession().setValid(false);
                throw e;
            }
            if (fireNotification)
            {
                fireNotification(event.getFlowConstruct(), result, next,
                    MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
            }
            return result;
        }
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }

    protected void fireNotification(FlowConstruct flowConstruct, MuleEvent event, MessageProcessor processor, int action)
    {
        if (notificationHandler != null
            && notificationHandler.isNotificationEnabled(MessageProcessorNotification.class))
        {
            notificationHandler.fireNotification(new MessageProcessorNotification(flowConstruct, event, processor, action));
        }
    }

    public final Object getAnnotation(QName name)
    {
        return annotations.get(name);
    }

    public final Map<QName, Object> getAnnotations()
    {
        return Collections.unmodifiableMap(annotations);
    }

    public synchronized final void setAnnotations(Map<QName, Object> newAnnotations)
    {
        annotations.clear();
        annotations.putAll(newAnnotations);
    }
}