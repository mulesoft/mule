/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.xa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConsumerProducerInvocationHandler implements InvocationHandler
{

    private Object target;

    public ConsumerProducerInvocationHandler(SessionInvocationHandler sessionInvocationHandler, Object target)
    {
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        try
        {
            return method.invoke(target, args);
        }
        catch (InvocationTargetException e)
        {
            throw e.getCause();
        }
    }

}
