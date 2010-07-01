/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.processor.MessageProcessor;

/**
 * <code>OutboundRouterCollection</code> is responsible for holding all outbound routers for a service service.
 */

public interface OutboundRouterCollection extends RouterCollection, MessageProcessor
{

    /**
     * Determines if any endpoints have been set on this router.
     */
    boolean hasEndpoints();
}
