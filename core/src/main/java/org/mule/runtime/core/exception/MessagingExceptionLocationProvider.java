/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import static org.mule.api.LocatedMuleException.INFO_LOCATION_KEY;
import static org.mule.api.LocatedMuleException.INFO_SOURCE_XML_KEY;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleEvent;
import org.mule.api.construct.MessageProcessorPathResolver;
import org.mule.api.execution.LocationExecutionContextProvider;
import org.mule.api.processor.MessageProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates location info to augment MessagingExceptions with.
 */
public class MessagingExceptionLocationProvider extends LocationExecutionContextProvider
{

    @Override
    public Map<String, Object> getContextInfo(MuleEvent event, MessageProcessor lastProcessed)
    {
        Map<String, Object> contextInfo = new HashMap<String, Object>();

        contextInfo.put(INFO_LOCATION_KEY, resolveProcessorRepresentation(event.getMuleContext().getConfiguration().getId(), getProcessorPath(event, lastProcessed), lastProcessed));
        if (lastProcessed instanceof AnnotatedObject)
        {
            String sourceXML = getSourceXML((AnnotatedObject) lastProcessed);
            if (sourceXML != null)
            {
                contextInfo.put(INFO_SOURCE_XML_KEY, sourceXML);
            }
        }

        return contextInfo;
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
