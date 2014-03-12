/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MulticastingRouter</code> will broadcast the current message to every endpoint
 * registered with the router.
 *
 * This class is deprecated since 3.5.0 and will be removed in Mule 4.0. Please use
 * {@link org.mule.routing.ScatterGatherRouter} instead.
 */

@Deprecated
public class MulticastingRouter extends AbstractSequenceRouter
{

    private static final Logger logger = LoggerFactory.getLogger(MulticastingRouter.class);

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        logger.warn("<all> router is deprecated since Mule 3.5.0 and will be removed in Mule 4. Please use <scatter-gather> instead");
    }

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
