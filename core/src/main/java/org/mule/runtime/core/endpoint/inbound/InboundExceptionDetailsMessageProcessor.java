/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.inbound;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.util.ObjectUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sets error message properties as specified by the transport based on the exception
 * type of the exception payload. This mechanism uses a transport properties file in
 * the META-INF/services/org/mule/config directory called
 * mule-exception-codes.properties. This property file maps the fully qualified class
 * names of exceptions to the value of the property that should be set. The name of
 * the property is defined by the error.code.property property in the same properties
 * file.
 */
public class InboundExceptionDetailsMessageProcessor implements MessageProcessor, MuleContextAware
{

    private static final Log logger = LogFactory.getLog(InboundExceptionDetailsMessageProcessor.class);

    protected Connector connector;
    private MuleContext muleContext;

    public InboundExceptionDetailsMessageProcessor(Connector connector)
    {
        this.connector = connector;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event != null && !VoidMuleEvent.getInstance().equals(event))
        {
            MuleMessage resultMessage = event.getMessage();
            if (resultMessage != null)
            {
                if (resultMessage.getExceptionPayload() != null)
                {
                    setExceptionDetails(resultMessage, connector, resultMessage.getExceptionPayload()
                        .getException());
                }
            }
        }
        return event;
    }

    /**
     * This method is used to set any additional and possibly transport specific
     * information on the return message where it has an exception payload.
     * 
     * @param message
     * @param exception
     */
    protected void setExceptionDetails(MuleMessage message, Connector connector, Throwable exception)
    {
        String propName = ExceptionHelper.getErrorCodePropertyName(connector.getProtocol(), muleContext);
        // If we dont find a error code property we can assume there are not
        // error code mappings for this connector
        if (propName != null)
        {
            String code = ExceptionHelper.getErrorMapping(connector.getProtocol(), exception.getClass(), muleContext);
            if (logger.isDebugEnabled())
            {
                logger.debug("Setting error code for: " + connector.getProtocol() + ", " + propName + "="
                             + code);
            }
            message.setOutboundProperty(propName, code);
        }
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
