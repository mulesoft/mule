/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.response;

import org.mule.routing.EventCorrelatorCallback;

/**
 * The router configured to handle reply messages when the {@link org.mule.config.annotations.endpoints.Reply} annotation
 * is used. If messages need to be correlated and aggregated, users can set the "aggregate" flag on the endpoint annotation.
 */
public class SingleResponseWithCallbackRouter extends AbstractResponseCallbackAggregator
{
    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new SingleResponseWithCallbackCorrelator(getCallbackMethod());
    }
}
