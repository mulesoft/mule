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

import org.mule.transport.jms.integration.JmsDurableTopicTestCase;
import org.mule.transport.jms.integration.JmsVendorConfiguration;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQDurableTopicTestCase extends JmsDurableTopicTestCase
{
    public ActiveMQDurableTopicTestCase()
    {
        super(new ActiveMQJmsConfiguration());
    }

    
}
