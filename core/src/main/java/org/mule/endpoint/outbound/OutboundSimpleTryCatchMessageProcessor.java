/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.transport.DispatchException;
import org.mule.processor.AbstractInterceptingMessageProcessor;

public class OutboundSimpleTryCatchMessageProcessor extends AbstractInterceptingMessageProcessor
{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            return processNext(event);
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
    }
}
