/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.reliability;



public class InboundMessageLossFlowTransactionsTestCase extends InboundMessageLossTransactionsTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "reliability/jdbc-connector.xml, reliability/inbound-message-loss-flow-transactions.xml";
    }
}
