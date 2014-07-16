/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;

/**
 * SPI for identifiable dynamic route resolvers
 */
public interface IdentifiableDynamicRouteResolver extends DynamicRouteResolver
{

    /**
     * Returns an identifier of the {@link org.mule.routing.DynamicRouteResolver}.
     *
     * @param event the event holding the message to route
     * @return an identifier of {@link org.mule.routing.DynamicRouteResolver}
     * @throws MessagingException
     */
    String getRouteIdentifier(MuleEvent event) throws MessagingException;
}
