/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.activemq;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;

public class JmsTransactedReconnectionTestCase extends JmsReconnectionTestCase
{

    protected UMOEndpoint getReceiverEndpoint(String URI) throws UMOException
    {
        MuleEndpoint ep = new MuleEndpoint(URI, true);
        ep.getTransactionConfig().setFactory(new JmsClientAcknowledgeTransactionFactory());
        ep.getTransactionConfig().setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        return ep;
    }

}
