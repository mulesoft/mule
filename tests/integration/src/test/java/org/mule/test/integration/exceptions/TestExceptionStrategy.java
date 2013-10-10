/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleEvent;
import org.mule.exception.AbstractMessagingExceptionStrategy;

public class TestExceptionStrategy extends AbstractMessagingExceptionStrategy
{
    public TestExceptionStrategy()
    {
        super(null);
    }
    
    @Override
    public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        MuleEvent result = super.handleException(exception, event);
        result.getMessage().setPayload("Ka-boom!");
        return result;
    }

    public boolean isRedeliver()
    {
        return false;
    }
}
