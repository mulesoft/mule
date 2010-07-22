/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.builder;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.object.ObjectFactory;
import org.mule.component.DefaultJavaComponent;
import org.mule.construct.SimpleService;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.object.PrototypeObjectFactory;

/**
 * Fluent API for the creation of a SimpleService.
 */
public class SimpleServiceBuilder extends AbstractFlowConstructBuilder<SimpleServiceBuilder, SimpleService>
{
    protected static final LegacyEntryPointResolverSet DEFAULT_ENTRY_POINT_RESOLVER_SET = new LegacyEntryPointResolverSet();

    protected Component component;

    @Override
    protected MessageExchangePattern getInboundMessageExchangePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

    public SimpleServiceBuilder serving(Class<?> componentClass)
    {
        return serving(new PrototypeObjectFactory(componentClass));
    }

    public SimpleServiceBuilder serving(ObjectFactory objectFactory)
    {
        return serving(new DefaultJavaComponent(objectFactory));
    }

    public SimpleServiceBuilder serving(Component component)
    {
        // TODO (DDO) support REST/WS annotated components

        if (component instanceof JavaComponent)
        {
            JavaComponent javaComponent = (JavaComponent) component;

            if (javaComponent.getEntryPointResolverSet() == null)
            {
                javaComponent.setEntryPointResolverSet(DEFAULT_ENTRY_POINT_RESOLVER_SET);
            }
        }

        this.component = component;
        return this;
    }

    @Override
    protected SimpleService buildFlowConstruct(MuleContext muleContext) throws MuleException
    {
        return new SimpleService(muleContext, name, buildMessageSource(muleContext), component);
    }

}
