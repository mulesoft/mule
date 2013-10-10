/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor.policy;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class TimingPolicy implements AroundPolicy
{

    protected final Log logger = LogFactory.getLog(getClass());

    public String getName()
    {
        return "simple timing policy";
    }

    public MuleEvent invoke(PolicyInvocation invocation) throws MuleException
    {
        final MuleEvent invocationEvent = invocation.getEvent();
        long startTime = System.currentTimeMillis();

        final MuleEvent result = invocation.proceed();

        long executionTime = System.currentTimeMillis() - startTime;
        if (logger.isInfoEnabled())
        {
            logger.info(String.format("%s took %dms to process event [%s]",
                                          invocationEvent.getFlowConstruct().getName(),
                                          executionTime, invocationEvent.getId()));
        }

        return result;
    }
}
