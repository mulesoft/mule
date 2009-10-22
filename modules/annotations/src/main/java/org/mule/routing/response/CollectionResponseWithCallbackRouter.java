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
 * is used and the "aggregate" flag  is set on the annotation. This router will correlate all messages with the same
 * correlation information set on them.
 */
public class CollectionResponseWithCallbackRouter extends AbstractResponseCallbackAggregator
{
    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new CollectionResponseWithCallbackCorrelator(getCallbackMethod(), muleContext);
    }
}