/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.activemq;

import org.mule.providers.jms.JmsConnector;

/**
 * ActiveMQ 4.x-specific JMS connector.
 */
public class ActiveMqJmsConnector extends JmsConnector
{
    /** Constructs a new ActiveMqJmsConnector. */
    public ActiveMqJmsConnector ()
    {
        setEagerConsumer(false);
        // TODO MULE-1409 better support for ActiveMQ 4.x temp destinations
    }
}