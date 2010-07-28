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
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.routing.CorrelationMode;
import org.mule.routing.MuleMessageInfoMapping;
import org.mule.util.SystemUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Depending on the {@link CorrelationMode} gives the message a correlation ID if
 * required.
 */
public class OutboundCorrelationPropertyMessageProcessor implements MessageProcessor
{

    protected transient Log logger = LogFactory.getLog(getClass());
    protected CorrelationMode correlationMode;

    public OutboundCorrelationPropertyMessageProcessor(CorrelationMode correlationMode)
    {
        this.correlationMode = correlationMode;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();
        if (event.getFlowConstruct() != null)
        {
            messageInfoMapping = event.getFlowConstruct().getMessageInfoMapping();
        }
        MuleMessage message = event.getMessage();

        if (correlationMode != CorrelationMode.NEVER)
        {
            boolean correlationSet = message.getCorrelationId() != null;
            if (correlationSet && (correlationMode == CorrelationMode.IF_NOT_SET))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("CorrelationId is already set to '" + message.getCorrelationId()
                                 + "' , not setting it again");
                }
                return event;
            }
            else if (correlationSet)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("CorrelationId is already set to '" + message.getCorrelationId()
                                 + "', but router is configured to overwrite it");
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("No CorrelationId is set on the message, will set a new Id");
                }
            }

            String correlation;
            correlation = messageInfoMapping.getCorrelationId(message);
            if (logger.isDebugEnabled())
            {
                logger.debug("Extracted correlation Id as: " + correlation);
            }

            if (logger.isDebugEnabled())
            {
                StringBuffer buf = new StringBuffer();
                buf.append("Setting Correlation info ");
                if (event.getEndpoint() instanceof OutboundEndpoint)
                {
                    buf.append(" for outbound endpoint: ").append(event.getEndpoint().getEndpointURI());
                }
                buf.append(SystemUtils.LINE_SEPARATOR).append("Id=").append(correlation);
                logger.debug(buf.toString());
            }
            message.setCorrelationId(correlation);
        }
        return event;
    }

}
