/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.UUID;

import java.util.List;

public class DynamicRouteResolverAdapter implements IdentifiableDynamicRouteResolver
{

    private final DynamicRouteResolver dynamicRouteResolver;
    private final String id;

    public DynamicRouteResolverAdapter(final DynamicRouteResolver dynamicRouteResolver)
    {
        this(dynamicRouteResolver, UUID.getUUID());
    }

    public DynamicRouteResolverAdapter(final DynamicRouteResolver dynamicRouteResolver, final String id)
    {
        this.dynamicRouteResolver = dynamicRouteResolver;
        this.id = id;
    }

    @Override
    public String getRouteIdentifier(MuleEvent event) throws MessagingException
    {

        if (dynamicRouteResolver instanceof IdentifiableDynamicRouteResolver)
        {
            return ((IdentifiableDynamicRouteResolver) dynamicRouteResolver).getRouteIdentifier(event);
        }

        return id;
    }

    @Override
    public List<MessageProcessor> resolveRoutes(MuleEvent event) throws MessagingException
    {
        return dynamicRouteResolver.resolveRoutes(event);
    }
}
