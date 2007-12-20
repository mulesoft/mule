/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.activemq;

import org.mule.util.object.ObjectFactory;
import org.mule.util.object.PrototypeObjectFactory;

import java.util.HashMap;
import java.util.Map;

public class ActiveMQXAJmsConnector extends ActiveMQJmsConnector
{
    public static final String ACTIVEMQ_XA_CONNECTION_FACTORY_CLASS = "org.apache.activemq.ActiveMQXAConnectionFactory";

    protected ObjectFactory/*<ConnectionFactory>*/ getDefaultConnectionFactory()
    {
        Map props = new HashMap();
        props.put("brokerURL", getBrokerURL());
        return new PrototypeObjectFactory(ACTIVEMQ_XA_CONNECTION_FACTORY_CLASS, props);
    }
}
