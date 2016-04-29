/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.outbound;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.ObjectUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OutboundLoggingMessageProcessor implements MessageProcessor
{
    protected final transient Log logger = LogFactory.getLog(getClass());

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("sending event: " + event);
        }

        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
