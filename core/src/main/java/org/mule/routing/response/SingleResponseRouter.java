/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.routing.correlation.SingleCorrelatorCallback;

/**
 * Handles single event responses from a replyTo address. If multiple responses will be received 
 * for a single invocation, the {@link ResponseCorrelationAggregator} should be used.
 */
public class SingleResponseRouter extends AbstractResponseAggregator
{
    @Override
    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new SingleCorrelatorCallback();
    }
}
