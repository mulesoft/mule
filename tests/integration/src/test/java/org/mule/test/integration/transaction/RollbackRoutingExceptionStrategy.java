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

import org.mule.DefaultExceptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;

/**
 * Will rollback the transaction in case a {@link org.mule.api.routing.RoutingException}
 * is encountered. Typically used with {@link org.mule.routing.outbound.TransactionJoiningRouter}
 * and configured on a connector.
 */
public class RollbackRoutingExceptionStrategy extends DefaultExceptionStrategy
{

    public void handleRoutingException(MuleMessage message, ImmutableEndpoint endpoint, Throwable t)
    {
        logger.debug("handleRoutingException: endpoint=" + endpoint + " message=" + message);
        defaultHandler(t);
        markTransactionForRollback();
        routeException(message, endpoint, t);
    }

}