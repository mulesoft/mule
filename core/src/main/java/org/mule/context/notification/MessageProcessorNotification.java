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
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.BeanUtils;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class MessageProcessorNotification extends ServerNotification implements BlockingServerEvent
{

    private static final long serialVersionUID = 1L;

    public static final int MESSAGE_PROCESSOR_PRE_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 1;
    public static final int MESSAGE_PROCESSOR_POST_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 2;

    static
    {
        registerAction("message processor pre invoke", MESSAGE_PROCESSOR_PRE_INVOKE);
        registerAction("message processor post invoke", MESSAGE_PROCESSOR_POST_INVOKE);
    }


    public MessageProcessorNotification(MuleEvent event, MessageProcessor processor, int action)
    {
        super(event, action, event.getFlowConstruct().getName());
        try {
            final String name = BeanUtils.getProperty(processor, "name");
            System.out.printf("++ %s > %s%n", event.getFlowConstruct().getName(), name != null ? name : processor);
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvocationTargetException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchMethodException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public MessageProcessorNotification(MuleEvent event, MessageProcessor processor, int action, String resourceIdentifier)
    {
        super(event, action, resourceIdentifier);
    }

    @Override
    public MuleEvent getSource() {
        return (MuleEvent) super.getSource();
    }
}
