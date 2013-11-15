/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.jms;

import org.mule.util.proxy.TargetInvocationHandler;

import java.lang.reflect.Method;

import bitronix.tm.resource.jms.MessageConsumerWrapper;

public class BitronixMessageConsumerInvocationHandler implements TargetInvocationHandler
{

    private final MessageConsumerWrapper messageConsumerWrapper;

    public BitronixMessageConsumerInvocationHandler(MessageConsumerWrapper messageConsumerWrapper)
    {
        this.messageConsumerWrapper = messageConsumerWrapper;
    }

    @Override
    public Object getTargetObject()
    {
        return messageConsumerWrapper;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        //TODO remove once BTM-132 gets fixed. BTM is to properly closing the message consumer and that generates a leak.
        if (method.getName().equals("close"))
        {
            messageConsumerWrapper.getMessageConsumer().close();
            return null;
        }
        return method.invoke(messageConsumerWrapper,args);
    }
}
