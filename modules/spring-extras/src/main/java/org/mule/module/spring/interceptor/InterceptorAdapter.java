/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.interceptor;

import org.mule.api.interceptor.Interceptor;
import org.mule.api.MuleMessage;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * This adapts an implementation of the {@link Interceptor} interface for use with the Spring AOP
 * interceptor framework.  Note that the Interceptor implementation can return null if it does not
 * want to change the result - the appropriate {@link MuleMessage} will then be constructed correctly
 * by Mule.
 */
public class InterceptorAdapter implements MethodInterceptor
{

    private Interceptor interceptor;

    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        InvocationAdapter adapter = new InvocationAdapter(invocation);
        MuleMessage message = interceptor.intercept(adapter);
        if (null == message)
        {
            return adapter.getResult();
        }
        else
        {
            return message;
        }
    }

    public void setInterceptor(Interceptor interceptor)
    {
        this.interceptor = interceptor;
    }

}
