/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import java.util.concurrent.atomic.AtomicInteger;

public class FilterCounter implements Filter
{
    public static AtomicInteger counter = new AtomicInteger();
    
    /**
     * Increments the counter if it passes the filter 
     */
    public boolean accept(MuleMessage message)
    {
        if ("true".equals(message.getInboundProperty("pass")))
        {
            counter.incrementAndGet();
            return true;
        }
        return false;
    }

    public boolean test(int arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

}


