/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.service.Service;
import org.mule.config.i18n.CoreMessages;

/**
 * This property extractor exposes mule context information as expressions. This can be context information about
 * the server itself such as the server id or about the current request such as the current service name.
 * <ul>
 * <li>serviceName - returns the name of the service currently processing the event.</li>
 * <li>modelName - returns the name of the model that hosts the current service</li>
 * <li>inboundEndpoint - returns the URI string of the endpoint that received the current messgae.</li>
 * <li>serverId - the Mule instance server Id.</li>
 * <li>clusterId - the Mule instance cluster Id.</li>
 * <li>domainId - the Mule instance domain Id.</li>
 * <li>workingDir - Mule's working directory.</li>
 * <li>homeDir - Mule's home directory</li>
 * </ul>
 */
public class MuleContextExpressionEvaluator implements ExpressionEvaluator, MuleContextAware
{
    public static final String NAME = "context";

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * Extracts a single property from the message
     *
     * @param expression the property expression or expression
     * @param message    the message to extract from
     * @return the result of the extraction or null if the property was not found
     */
    public Object evaluate(String expression, MuleMessage message)
    {
        if (expression.equals("serviceName"))
        {
            return getEventContext().getFlowConstruct().getName();
        }
        else if (expression.equals("modelName"))
        {
            if (getEventContext().getFlowConstruct() instanceof Service)
            {
                return ((Service) getEventContext().getFlowConstruct()).getModel().getName();
            }
            else
            {
                throw new UnsupportedOperationException("The 'modelName' function can only be used with Service");
            }
        }
        else if (expression.equals("inboundEndpoint"))
        {
            return getEventContext().getEndpointURI();
        }
        else if (expression.equals("serverId"))
        {
            return getMuleContext().getConfiguration().getId();
        }
        else if (expression.equals("clusterId"))
        {
            return getMuleContext().getClusterId();
        }
        else if (expression.equals("domainId"))
        {
            return getMuleContext().getConfiguration().getDomainId();
        }
        else if (expression.equals("workingDir"))
        {
            return getMuleContext().getConfiguration().getWorkingDirectory();
        }
        else if (expression.equals("homeDir"))
        {
            return getMuleContext().getConfiguration().getMuleHomeDirectory();
        }
        else
        {
            throw new IllegalArgumentException(expression);
        }
    }

    protected MuleContext getMuleContext()
    {
        return muleContext;
    }

    protected MuleEventContext getEventContext()
    {
        if(RequestContext.getEventContext()==null)
        {
             throw new MuleRuntimeException(CoreMessages.objectIsNull("MuleEventContext"));
        }
        else
        {
            return RequestContext.getEventContext();
        }
    }

    /**
     * Gts the name of the object
     *
     * @return the name of the object
     */
    public String getName()
    {
        return NAME;
    }

}
