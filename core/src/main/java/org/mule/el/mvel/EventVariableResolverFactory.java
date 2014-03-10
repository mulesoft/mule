/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.mvel2.integration.VariableResolver;

public class EventVariableResolverFactory extends MessageVariableResolverFactory
{

    private static final long serialVersionUID = -6819292692339684915L;

    private final String FLOW = "flow";
    private MuleEvent event;

    public EventVariableResolverFactory(MuleEvent event)
    {
        super(event.getMessage());
        this.event = event;
    }

    @Override
    public VariableResolver getVariableResolver(String name)
    {
        if (event != null)
        {
            if (FLOW.equals(name))
            {
                return new MuleImmutableVariableResolver<FlowContext>(FLOW, (new FlowContext(
                    event.getFlowConstruct())), null);
            }
            else if (MVELExpressionLanguageContext.MULE_EVENT_INTERNAL_VARIABLE.equals(name))
            {
                return new MuleImmutableVariableResolver<MuleEvent>(
                    MVELExpressionLanguageContext.MULE_EVENT_INTERNAL_VARIABLE, event, null);
            }
            else
            {
                return super.getVariableResolver(name);
            }
        }
        return null;
    }

    @Override
    public boolean isTarget(String name)
    {
        return FLOW.equals(name) || MVELExpressionLanguageContext.MULE_EVENT_INTERNAL_VARIABLE.equals(name)
               || super.isTarget(name);
    }

    @Override
    public boolean isResolveable(String name)
    {
        return isTarget(name);
    }

    public static class FlowContext
    {
        private FlowConstruct flowConstruct;

        public FlowContext(FlowConstruct flowConstruct)
        {
            this.flowConstruct = flowConstruct;
        }

        public String getName()
        {
            return flowConstruct.getName();
        }
    }
}
