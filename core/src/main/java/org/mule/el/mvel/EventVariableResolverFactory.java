/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;

import org.mvel2.ParserContext;

class EventVariableResolverFactory extends MessageVariableResolverFactory
{

    private static final long serialVersionUID = -6819292692339684915L;

    private MuleEvent event;

    public EventVariableResolverFactory(ParserContext parserContext, MuleContext muleContext, MuleEvent event)
    {
        super(parserContext, muleContext, event.getMessage());
        this.event = event;

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
