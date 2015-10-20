/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.LocatedMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.construct.MessageProcessorPathResolver;
import org.mule.api.execution.LocationExecutionContextProvider;
import org.mule.api.processor.MessageProcessor;

import java.util.Collections;
import java.util.Map;

/**
 * Generates location info to augment MessagingExceptions with.
 */
public class MessagingExceptionLocationProvider extends LocationExecutionContextProvider
{

    @Override
    public Map<String, Object> getContextInfo(MuleEvent event, MessageProcessor lastProcessed)
    {
        return Collections.<String, Object> singletonMap(LocatedMuleException.INFO_LOCATION_KEY,
                resolveProcessorRepresentation(event.getMuleContext().getConfiguration().getId(), getProcessorPath(event, lastProcessed), lastProcessed));
    }

    protected String getProcessorPath(MuleEvent event, MessageProcessor lastProcessed)
    {
        if (event.getFlowConstruct() != null && event.getFlowConstruct() instanceof MessageProcessorPathResolver)
        {
            return ((MessageProcessorPathResolver) event.getFlowConstruct()).getProcessorPath(lastProcessed);
        }
        else
        {
            return lastProcessed.toString();
        }
    }
}
