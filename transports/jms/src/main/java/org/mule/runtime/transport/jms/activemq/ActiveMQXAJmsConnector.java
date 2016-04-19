/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.activemq;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.ClassUtils;

import javax.jms.ConnectionFactory;

public class ActiveMQXAJmsConnector extends ActiveMQJmsConnector
{
    public static final String ACTIVEMQ_XA_CONNECTION_FACTORY_CLASS = "org.apache.activemq.ActiveMQXAConnectionFactory";

    public ActiveMQXAJmsConnector(MuleContext context)
    {
        super(context);
    }
    
    @Override
    protected ConnectionFactory getDefaultConnectionFactory() throws Exception
    {
        ConnectionFactory connectionFactory = (ConnectionFactory)
                ClassUtils.instanciateClass(ACTIVEMQ_XA_CONNECTION_FACTORY_CLASS, getBrokerURL());
        applyVendorSpecificConnectionFactoryProperties(connectionFactory);
        return connectionFactory;
    }
}
