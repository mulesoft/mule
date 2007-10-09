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
import org.mule.config.spring.parsers.delegate.AbstractSerialDelegatingDefinitionParser;
import org.mule.config.spring.parsers.generic.GrandchildDefinitionParser;
import org.mule.routing.inbound.ForwardingConsumer;
import org.mule.util.object.SingletonObjectFactory;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * In addition to a ForwardingConsumer router, implicitly create a NullComponent service as a placeholder.
 *
 * We use AbstractSerialDelegatingDefinitionParser to be able to create 2 beans from a single element.
 * 
 * We use a ParentDefinitionParser for the NullComponent, because it needs to be set not on 
 * <inbound-router> (the surrounding element), but on <service> (one level up).
 * 
 *       <service name="BridgeOut">
 *           <inbound-router>
 *               <inbound-endpoint address="tcp://localhost:9994" transformer-ref="NoAction"/>
 *               <forwarding-router/>
 *           </inbound-router>
 *       </service>
 */
public class ForwardingRouterDefinitionParser extends AbstractSerialDelegatingDefinitionParser
{
    public ForwardingRouterDefinitionParser()
    {
        super();
        addDelegate(new RouterDefinitionParser("router", ForwardingConsumer.class));
        addDelegate(new NullComponentPlaceholder());
    }
    
    class NullComponentPlaceholder extends GrandchildDefinitionParser
    {
        public NullComponentPlaceholder()
        {
            super("serviceFactory", SingletonObjectFactory.class);
        }
        
        protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
        {
            builder.addPropertyValue("objectClassName", NullComponent.class.getName());
            super.parseChild(element, parserContext, builder);
        }
    }
}


