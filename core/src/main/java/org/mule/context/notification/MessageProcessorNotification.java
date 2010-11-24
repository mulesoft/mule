/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;

import java.lang.reflect.Method;

/**
 *
 */
public class MessageProcessorNotification extends ServerNotification implements BlockingServerEvent
{

    private static final long serialVersionUID = 1L;

    public static final int MESSAGE_PROCESSOR_PRE_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 1;
    public static final int MESSAGE_PROCESSOR_POST_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 2;

    private transient MessageProcessor processor;

    static
    {
        registerAction("message processor pre invoke", MESSAGE_PROCESSOR_PRE_INVOKE);
        registerAction("message processor post invoke", MESSAGE_PROCESSOR_POST_INVOKE);
    }


    public MessageProcessorNotification(MuleEvent event, MessageProcessor processor, int action)
    {
        super(event, action, event.getFlowConstruct() != null ? event.getFlowConstruct().getName() : null);

        try
        {
            String mpName;
            // TODO would love to see MP.getName() in the API. Avoid BeanUtils.getProperty() overhead here
            final Method method = processor.getClass().getMethod("getName");
            if (method == null)
        {
            // no such method, fallback to MP class name + NamedObject
            mpName = ObjectUtils.toString(processor);
        }
        else
        {
            // invoke existing getName(), but provide same fallback if it returned nothing
            mpName = ObjectUtils.toString(method.invoke(processor), ObjectUtils.toString(processor));
        }


            System.out.printf("++ %s > %s%n", event.getFlowConstruct().getName(), mpName);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public MuleEvent getSource()
    {
        return (MuleEvent) super.getSource();
    }

    public MessageProcessor getProcessor()
    {
        return processor;
    }
}
