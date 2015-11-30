/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MuleEventCheckAnswer extends Object implements Answer<MuleEvent>
{
    private MuleEvent returnEvent;
    private MuleException exceptionToThrow;

    public MuleEventCheckAnswer()
    {
        this((MuleEvent) null);
    }

    public MuleEventCheckAnswer(MuleEvent returnEvent)
    {
        this.returnEvent = returnEvent;
    }

    public MuleEventCheckAnswer(MuleException ex)
    {
        this.exceptionToThrow = ex;
    }

    @Override
    public MuleEvent answer(InvocationOnMock invocation) throws Throwable
    {
        if (exceptionToThrow != null)
        {
            throw exceptionToThrow;
        }
        else
        {
            return checkInvocation(invocation);
        }
    }

    private MuleEvent checkInvocation(InvocationOnMock invocation)
    {
        Object[] arguments = invocation.getArguments();
        assertEquals(1, arguments.length);
        assertTrue(arguments[0] instanceof MuleEvent);

        return returnEvent;
    }
}
