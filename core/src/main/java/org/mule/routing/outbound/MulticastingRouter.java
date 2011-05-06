/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
