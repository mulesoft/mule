/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionEvaluator;
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
public class EndpointInfoExpressionEvaluator implements ExpressionEvaluator, MuleContextAware
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
