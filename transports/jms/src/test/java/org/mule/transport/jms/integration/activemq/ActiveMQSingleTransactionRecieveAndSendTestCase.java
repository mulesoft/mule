/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration.activemq;

import org.mule.transport.jms.integration.JmsSingleTransactionRecieveAndSendTestCase;
import org.mule.transport.jms.integration.JmsVendorConfiguration;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.api.transport.Connector;

/**
 * TODO
 */
public class ActiveMQSingleTransactionRecieveAndSendTestCase extends JmsSingleTransactionRecieveAndSendTestCase
{
    public ActiveMQSingleTransactionRecieveAndSendTestCase()
    {
        super(new ActiveMQJmsConfiguration());
    }
}
