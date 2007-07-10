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

import org.mule.test.integration.providers.jms.AbstractJmsTransactionFunctionalTest;

import javax.jms.Session;

public class JmsClientAcknowledgeTransactionFunctionalTestCase extends AbstractJmsTransactionFunctionalTest
{
    protected String getConfigResources()
    {
        return "activemq-client-ack.xml," + super.getConfigResources();
    }

    //@Override
    protected int getAcknowledgementMode()
    {
        return Session.CLIENT_ACKNOWLEDGE;
    }

    public void testSendTransactedRollback() throws Exception
    {
        // Rollback not allowed for client acknowledge
    }

    public void testTransactedRedeliveryToDLDestination() throws Exception
    {
        // messages are not marked for redelivery in Client Ack mode
    }
}
