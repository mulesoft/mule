/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.esb;

import org.mule.examples.loanbroker.tests.AbstractLoanBrokerTestCase;

import org.apache.activemq.broker.BrokerService;

public abstract class AbstractLoanBrokerEsbTestCase extends AbstractLoanBrokerTestCase
{
    private BrokerService msgBroker = null;
    
    protected void suitePreSetUp() throws Exception
    {
        super.suitePreSetUp();
        // Start up the ActiveMQ message broker.
        msgBroker = new BrokerService();
        msgBroker.addConnector("tcp://localhost:61616");
        msgBroker.start();
    }

    protected void suitePostTearDown() throws Exception
    {
        if (msgBroker != null)
        {
            msgBroker.stop();
        }
        super.suitePostTearDown();
    }
}
