/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 * <code>MulticastingRouter</code> will broadcast the current message to every endpoint
 * registered with the router.
 */

public class MulticastingRouter extends AbstractSequenceRouter
{

    /**
     * Indicates that this router always routes messages to all the configured
     * endpoints no matters what a given response is.
     */
    @Override
    protected boolean continueRoutingMessageAfter(MuleEvent response) throws MuleException
    {
        return true;
    }
}
