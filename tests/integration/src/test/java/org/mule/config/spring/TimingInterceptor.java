/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.interceptor.Interceptor;
import org.mule.api.interceptor.Invocation;
import org.mule.api.MuleMessage;
import org.mule.api.MuleException;

public class TimingInterceptor implements Interceptor
{

    public static final long UNCALLED = -1L;
    private long interval = UNCALLED;

    public MuleMessage intercept(Invocation invocation) throws MuleException
    {
        long start = System.currentTimeMillis();
        try
        {
            // call the component
            invocation.execute();
            // let the framework construct the correct message
            return null;
        }
        finally
        {
            interval = System.currentTimeMillis() - start;
        }
    }

    public long getInterval()
    {
        return interval;
    }

}
