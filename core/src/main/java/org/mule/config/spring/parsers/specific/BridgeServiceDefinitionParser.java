/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.components.simple.NullComponent;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyChildDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.impl.endpoint.InboundEndpoint;
import org.mule.routing.inbound.ForwardingConsumer;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.UMORouter;

import java.util.Iterator;
import java.util.List;

/**
 * Create a service with a {@link org.mule.components.simple.NullComponent}.
 *
 * <p>We do most of this at the config level, but need to use a simple adapter on the Descriptor to
 * handle the direct setting of endpoints.</p>
 */
public class BridgeServiceDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    public static final String NAME = AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME;

    public BridgeServiceDefinitionParser()
    {
        addDelegate(new ComponentDefinitionParser(BridgeDescriptor.class));
        addChildDelegate(new SimplePojoServiceDefinitionParser(NullComponent.class)).addIgnored(NAME);
        addChildDelegate(new InboundForwardRouterDefinitionParser()).addIgnored(NAME);
    }

    private static class InboundForwardRouterDefinitionParser
    extends AbstractSingleParentFamilyChildDefinitionParser
    {

        public InboundForwardRouterDefinitionParser()
        {
            addChildDelegate(new ChildDefinitionParser("inboundRouter", InboundRouterCollection.class));
            addChildDelegate(new ChildDefinitionParser("router", ForwardingConsumer.class));
        }

    }

    private static class BridgeDescriptor extends SedaComponent
    {

        public void setEndpoints(List list)
        {
            Iterator endpoint = list.iterator();
            while (endpoint.hasNext())
            {
                addEndpoint((UMOImmutableEndpoint) endpoint.next());
            }
        }

        private void addEndpoint(UMOImmutableEndpoint endpoint)
        {
            if (endpoint instanceof InboundEndpoint)
            {
                getInboundRouter().addEndpoint(endpoint);
            }
            else
            {
                getOutboundPassThroughRouter().addEndpoint(endpoint);
            }
        }

        private OutboundPassThroughRouter getOutboundPassThroughRouter()
        {
            if (null == getOutboundRouter())
            {
                setOutboundRouter(new OutboundRouterCollection());
            }
            else
            {
                for (Iterator routers = getOutboundRouter().getRouters().iterator(); routers.hasNext();)
                {
                    UMORouter router = (UMORouter) routers.next();
                    if (router instanceof OutboundPassThroughRouter)
                    {
                        return (OutboundPassThroughRouter) router;
                    }
                }
            }
            OutboundPassThroughRouter router = new OutboundPassThroughRouter();
            getOutboundRouter().addRouter(router);
            return router;
        }

    }

}
