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

import org.mule.api.MuleException;
import org.mule.interceptor.RequestContextInvocation;
import org.mule.interceptor.InterceptorException;

import org.aopalliance.intercept.MethodInvocation;

public class InvocationAdapter extends RequestContextInvocation
{

    private MethodInvocation invocation;
    private Object result;

    public InvocationAdapter(MethodInvocation invocation)
    {
        this.invocation = invocation;
    }

    public Object execute() throws MuleException
    {
        try
        {
            result = invocation.proceed();
            return result;
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Throwable e)
        {
            throw new InterceptorException(e);
        }
    }

    public Object getResult()
    {
        return result;
    }

}
