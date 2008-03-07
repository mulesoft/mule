/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.routing.ResponseRouter;
import org.mule.config.MuleConfiguration;
import org.mule.routing.AbstractRouter;
import org.mule.routing.CorrelationPropertiesExpressionEvaluator;
import org.mule.util.ClassUtils;
import org.mule.util.expression.ExpressionEvaluator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractResponseRouter</code> is a base class for all Response Routers
 */

public abstract class AbstractResponseRouter extends AbstractRouter implements ResponseRouter
{
    protected final Log logger = LogFactory.getLog(getClass());

    private int timeout = MuleConfiguration.DEFAULT_TIMEOUT;

    private boolean failOnTimeout = true;

    protected ExpressionEvaluator propertyExtractor = new CorrelationPropertiesExpressionEvaluator();

    public ExpressionEvaluator getPropertyExtractor()
    {
        return propertyExtractor;
    }

    public void setPropertyExtractor(ExpressionEvaluator propertyExtractor)
    {
        this.propertyExtractor = propertyExtractor;
    }

    /**
     * A digester callback to configure a custom correlation extractor.
     * 
     * @param className correlation extractor fully qualified class name
     */
    public void setPropertyExtractorAsString(String className)
    {
        try
        {
            this.propertyExtractor = (ExpressionEvaluator) ClassUtils.instanciateClass(className, null,
                getClass());
        }
        catch (Exception ex)
        {
            throw (IllegalArgumentException) new IllegalArgumentException(
                "Couldn't instanciate property extractor class " + className
                ).initCause(ex);
        }
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    /**
     * Extracts a 'Correlation Id' from a reply message. The correlation Id does not
     * have to be the Message Correlation Id. It can be extracted from the message
     * payload if desired.
     * 
     * @param message a received reply message
     * @return the correlation Id for this message
     */
    protected Object getReplyAggregateIdentifier(MuleMessage message)
    {
        return propertyExtractor.evaluate(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
    }

    /**
     * Extracts a Group identifier from the current event. When an event is received
     * with a group identifier not registered with this router, a new group is
     * created. The id returned here can be a correlationId or some custom
     * aggregation Id. This implementation uses the Unique Message Id of the
     * MuleMessage being returned a
     * 
     * @param message A response messages received on the response router endpoint
     * @return an aggregation Id for this event
     */
    protected Object getCallResponseAggregateIdentifier(MuleMessage message)
    {
        return propertyExtractor.evaluate(MuleProperties.MULE_MESSAGE_ID_PROPERTY, message);
    }

    public boolean isFailOnTimeout()
    {
        return failOnTimeout;
    }

    public void setFailOnTimeout(boolean failOnTimeout)
    {
        this.failOnTimeout = failOnTimeout;
    }
}
