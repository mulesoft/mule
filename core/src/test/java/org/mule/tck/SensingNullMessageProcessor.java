/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

public class SensingNullMessageProcessor implements MessageProcessor
{
    public MuleEvent event;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        this.event = event;
        if (event.getEndpoint().getExchangePattern().hasResponse())
        {
            return event;
        }
        else
        {
            return null;
        }
    }

    public void clear()
    {
        event = null;
    }
}
