/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.VoidMuleEvent;
import org.mule.api.AnnotatedObject;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.ServerNotificationHandler;
import org.mule.api.processor.InternalMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.util.NotificationUtils;
import org.mule.util.ObjectUtils;

import java.util.Arrays;
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
        implements MessageProcessor, MuleContextAware, AnnotatedObject, MessageProcessorContainer
{

    protected Log logger = LogFactory.getLog(getClass());

    protected ServerNotificationHandler notificationHandler;
    private MessageProcessorExecutionTemplate messageProcessorExecutorWithoutNotifications = MessageProcessorExecutionTemplate.createExceptionTransformerExecutionTemplate();
    private MessageProcessorExecutionTemplate messageProcessorExecutorWithNotifications = MessageProcessorExecutionTemplate.createExecutionTemplate();
    protected MuleContext muleContext;
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        notificationHandler = muleContext.getNotificationManager();
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
        else if (VoidMuleEvent.getInstance().equals(event))
        {
            return event;
        }
        else
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
            }

            MessageProcessorExecutionTemplate executionTemplateToUse = (!(next instanceof MessageProcessorChain)) ? messageProcessorExecutorWithNotifications : messageProcessorExecutorWithoutNotifications;

            try
            {
                return executionTemplateToUse.execute(next, event);
            }
            catch (MessagingException e)
            {
                event.getSession().setValid(false);
                throw e;
            }
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

    protected boolean isEventValid(MuleEvent event)
    {
        return event != null && !VoidMuleEvent.getInstance().equals(event);
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        if (next instanceof InternalMessageProcessor)
        {
            return;
        }
        if (next instanceof MessageProcessorChain)
        {
            NotificationUtils.addMessageProcessorPathElements(((MessageProcessorChain) next).getMessageProcessors(), pathElement.getParent());
        }
        else if (next != null)
        {
            NotificationUtils.addMessageProcessorPathElements(Arrays.asList(next), pathElement.getParent());
        }
    }
}
