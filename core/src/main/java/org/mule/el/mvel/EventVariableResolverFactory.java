/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;

import org.mvel2.ParserContext;

class EventVariableResolverFactory extends MessageVariableResolverFactory
{

    private static final long serialVersionUID = -6819292692339684915L;

    public EventVariableResolverFactory(ParserContext parserContext, MuleContext muleContext, MuleEvent event)
    {
        super(parserContext, muleContext, event.getMessage());
        addFinalVariable("flow", new FlowContext(event.getFlowConstruct()));
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
