/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import static org.apache.commons.lang.StringUtils.isEmpty;

import org.mule.api.MuleEvent;

import org.slf4j.Logger;

/**
 * Helper class to reuse message content logging for troubleshooting using the logs.
 */
public class MuleEventLogger
{

    private final Logger logger;

    public MuleEventLogger(Logger logger)
    {
        this.logger = logger;
    }

    /**
     * Logs the event message payload type, the payload as string and the message properties.
     * @param muleEvent event to log.
     */
    public void logContent(MuleEvent muleEvent)
    {
        logger.error("Message content type is " + muleEvent.getMessage().getPayload().getClass());
        logger.error("Message content is " + muleEvent.getMessage());
        try
        {
            String payloadAsString = muleEvent.getMessage().getPayloadAsString();
            logger.error("Message payload is " + (isEmpty(payloadAsString) ? "EMPTY CONTENT" : payloadAsString));
        }
        catch (Exception e)
        {
            //just skip the logging message if we couldn't convert the payload to string.
        }
    }

}
