/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.AbstractEndpointBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Looks up information about a global endpoint
 *
 * @see org.mule.api.expression.ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class EndpointInfoExpressionEvaluator extends AbstractExpressionEvaluator implements MuleContextAware
{
    public static final String NAME = "endpoint";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(EndpointInfoExpressionEvaluator.class);

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public Object evaluate(String expression, MuleMessage message)
    {
        int i = expression.indexOf(".");
        String endpointName;
        String property;
        if(i > 0)
        {
            endpointName = expression.substring(0, i);
            property = expression.substring(i + 1);
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.expressionMalformed(expression, getName()).getMessage());
        }

        AbstractEndpointBuilder eb = (AbstractEndpointBuilder)muleContext.getRegistry().lookupEndpointBuilder(endpointName);
        if(eb!=null)
        {

            if(property.equalsIgnoreCase("address"))
            {
                return eb.getEndpointBuilder().getEndpoint().getAddress();
            }
            else //TODO more properties
            {
                throw new IllegalArgumentException(CoreMessages.expressionInvalidForProperty(property, expression).getMessage());
            }
        }
        else
        {
            logger.warn("There is no endpoint registered with name: " + endpointName);
            return null;
        }
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }

}
