/*
 * $Id:AbstractExternalTransactionTestCase.java 8215 2007-09-05 16:56:51Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.process;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;

class RethrowExceptionInterceptor implements ProcessingInterceptor<MuleEvent>
{

    private final ProcessingInterceptor<MuleEvent> next;

    public RethrowExceptionInterceptor(ProcessingInterceptor<MuleEvent> next)
    {
        this.next = next;
    }

    @Override
    public MuleEvent execute(ProcessingCallback<MuleEvent> processingCallback) throws Exception
    {
        try
        {
            return this.next.execute(processingCallback);
        }
        catch (MessagingException e)
        {
            if (e.handled())
            {
                return e.getEvent();
            }
            throw e;
        }
    }
}
