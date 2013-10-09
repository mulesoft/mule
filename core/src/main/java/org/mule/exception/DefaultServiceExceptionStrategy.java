/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.exception;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.management.stats.ServiceStatistics;

/**
 * This is the default exception handler for flows and services. The handler logs errors 
 * and will forward the message and exception to an exception endpoint if one is set 
 * on this Exception strategy.  If an endpoint is configured via the <default-exception-strategy> 
 * element, a Dead Letter Queue pattern is assumed and so the transaction will commit.
 * Otherwise, the transaction will rollback, possibly causing the source message to be 
 * redelivered (depends on the transport).
 *
 * @deprecated use {@link org.mule.exception.DefaultMessagingExceptionStrategy} instead
 */
@Deprecated
public class DefaultServiceExceptionStrategy extends DefaultMessagingExceptionStrategy
{
    public DefaultServiceExceptionStrategy()
    {
        this(null);
    }

    public DefaultServiceExceptionStrategy(MuleContext muleContext)
    {
        super(muleContext);
    }

    protected FlowConstructStatistics getFlowConstructStatistics(FlowConstruct flowConstruct)
    {
        if (flowConstruct != null )
        {
            return flowConstruct.getStatistics();
        }
        else
        {
            //this can happen, e.g. with event constructed to handle exceptions
            // logger.fatal("The Default Service Exception Strategy has been invoked but there is no current flow construct on the context. Please report this to dev@mule.codehaus.org");
            return null;
        }
    }

    protected ServiceStatistics getServiceStatistics(FlowConstruct flowConstruct)
    {
        FlowConstructStatistics stats = getFlowConstructStatistics(flowConstruct);
        if (!(stats instanceof ServiceStatistics))
        {
            //this should never happen, but JIC
            logger.fatal("The Default Service Exception Strategy has been invoked but there is no current service on the context. Please report this to dev@mule.codehaus.org");
            return null;
        }
        return (ServiceStatistics) stats;
    }
}
