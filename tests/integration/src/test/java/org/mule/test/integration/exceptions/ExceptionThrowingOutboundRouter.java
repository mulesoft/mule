/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.MessageFactory;
import org.mule.routing.outbound.OutboundPassThroughRouter;

public class ExceptionThrowingOutboundRouter extends OutboundPassThroughRouter
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        throw new RoutingException(MessageFactory.createStaticMessage("dummyException"), event, null);
    }
}


