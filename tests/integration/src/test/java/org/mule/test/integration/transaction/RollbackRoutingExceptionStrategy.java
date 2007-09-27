/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import org.mule.impl.DefaultExceptionStrategy;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * Will rollback the transaction in case a {@link org.mule.umo.routing.RoutingException}
 * is encountered. Typically used with {@link org.mule.routing.outbound.TransactionJoiningRouter}
 * and configured on a connector.
 */
public class RollbackRoutingExceptionStrategy extends DefaultExceptionStrategy
{

    public void handleRoutingException(UMOMessage message, UMOImmutableEndpoint endpoint, Throwable t)
    {
        logger.debug("handleRoutingException: endpoint=" + endpoint + " message=" + message);
        defaultHandler(t);
        markTransactionForRollback();
        routeException(message, endpoint, t);
    }

}