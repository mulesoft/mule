/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

public class ResponseMessageProcessorAdapter extends AbstractResponseMessageProcessor
{

    protected MessageProcessor responseProcessor;

    public ResponseMessageProcessorAdapter()
    {
        super();
    }

    public ResponseMessageProcessorAdapter(MessageProcessor responseProcessor)
    {
        super();
        this.responseProcessor = responseProcessor;
    }

    public void setProcessor(MessageProcessor processor)
    {
        this.responseProcessor = processor;
    }

    @Override
    protected MuleEvent processResponse(MuleEvent event) throws MuleException
    {
        if (responseProcessor == null)
        {
            return event;
        }
        else
        {
            return responseProcessor.process(event);
        }
    }

}
