/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.exception.AbstractExceptionListener;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.NullPayload;

import java.beans.ExceptionListener;

public class ExceptionHandlingMessageProcessor extends AbstractInterceptingMessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            return processNext(event);
        }
        catch (Exception e)
        {
            ExceptionListener exceptionListener;
            if (event.getFlowConstruct() != null)
            {
                exceptionListener = event.getFlowConstruct().getExceptionListener();
            }
            else
            {
                logger.warn("FlowContruct is not set on MuleEvent, this is probably a bug");
                exceptionListener = event.getMuleContext().getExceptionListener();
            }            
            exceptionListener.exceptionThrown(e);

            // TODO We should really have MuleExceptionHandler interface which returns a MuleEvent instead of "void exceptionThrown()"
            if (exceptionListener instanceof AbstractExceptionListener)
            {
                return new DefaultMuleEvent(((AbstractExceptionListener) exceptionListener).getReturnMessage(e), event);
            }
            else
            {
                event.getMessage().setPayload(NullPayload.getInstance());
                event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
                return event;
            }
        }
    }
}
