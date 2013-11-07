/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import org.apache.activemq.broker.BrokerService;

public class JmsBrokerSetUp implements TransactionalTestSetUp
{

    private final int port;
    private BrokerService broker;

    public JmsBrokerSetUp(int port)
    {
        this.port = port;
    }

    @Override
    public void initialize() throws Exception
    {
        broker = new BrokerService();
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.addConnector("tcp://localhost:" + port);
        broker.start();
    }

    @Override
    public void finalice() throws Exception
    {
        try
        {
            broker.stop();
        }
        catch (Exception e)
        {
        }
    }
}
