/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.xa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ConsumerProducerInvocationHandler implements InvocationHandler
{

    private Object target;
    private SessionInvocationHandler sessionInvocationHandler;

    public ConsumerProducerInvocationHandler(SessionInvocationHandler sessionInvocationHandler, Object target)
    {
        this.sessionInvocationHandler = sessionInvocationHandler;
        this.target = target;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (ConnectionFactoryWrapper.logger.isDebugEnabled())
        {
            ConnectionFactoryWrapper.logger.debug("Invoking " + method);
        }
        if (!method.getName().equals("close"))
        {
            sessionInvocationHandler.enlist();
        }
        return method.invoke(target, args);
    }
}
