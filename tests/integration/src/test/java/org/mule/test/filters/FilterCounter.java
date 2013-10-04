/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


