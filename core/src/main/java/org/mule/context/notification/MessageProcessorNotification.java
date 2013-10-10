/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.api.NamedObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;

import java.lang.reflect.Method;

public class MessageProcessorNotification extends ServerNotification implements BlockingServerEvent
{

    private static final long serialVersionUID = 1L;

    public static final int MESSAGE_PROCESSOR_PRE_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 1;
    public static final int MESSAGE_PROCESSOR_POST_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 2;

    private final transient MessageProcessor processor;
    private String messageProcessorName;

    static
    {
        registerAction("message processor pre invoke", MESSAGE_PROCESSOR_PRE_INVOKE);
        registerAction("message processor post invoke", MESSAGE_PROCESSOR_POST_INVOKE);
    }


    public MessageProcessorNotification(FlowConstruct flowConstruct, MuleEvent event, MessageProcessor processor, int action)
    {
        super(event, action, flowConstruct != null ? flowConstruct.getName() : null);

        this.processor = processor;

        // can't extract it to a method and still kepp the field final, have to leave it inline
        try
        {
            // TODO would love to see MP.getName() in the API. Avoid BeanUtils.getProperty() overhead here
            
            try
            {
                final Method method = processor.getClass().getMethod("getName");
                // invoke existing getName(), but provide same fallback if it returned nothing
                messageProcessorName = ObjectUtils.toString(method.invoke(processor), toString(processor));
            }
            catch (NoSuchMethodException e)
            {
                // no such method, fallback to MP class name + NamedObject
                messageProcessorName = toString(processor);
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
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

    public String getFriendlyProcessorName()
    {
        return messageProcessorName;
    }

    protected String toString(Object obj)
    {
        if (obj == null)
        {
            return "";
        }

        String name;
        if (obj instanceof NamedObject)
        {
            name = String.format("%s '%s'", obj.getClass().getName(), ((NamedObject) obj).getName());
        }
        else
        {
            name = ObjectUtils.identityToString(obj);
        }
        return name;
    }
}
