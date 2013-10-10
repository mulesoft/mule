/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


