/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.xa;

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
