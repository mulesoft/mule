/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.test;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

public class TestConnectionFactory implements QueueConnectionFactory
{
    private String providerProperty = "NOT_SET";
    private String connectionFactoryProperty = "NOT_SET";

    public Connection createConnection() throws JMSException
    {
        return null;
    }

    public Connection createConnection(String string, String string1) throws JMSException
    {
        return null;
    }

    public String getProviderProperty()
    {
        return providerProperty;
    }

    /**
     * Should NOT be called.
     */
    public void setProviderProperty(final String providerProperty)
    {
        throw new IllegalStateException("Should never be called.");
    }

    public String getConnectionFactoryProperty()
    {
        return connectionFactoryProperty;
    }

    /**
     * MUST be called
     */
    public void setConnectionFactoryProperty(final String connectionFactoryProperty)
    {
        this.connectionFactoryProperty = connectionFactoryProperty;
    }

    public QueueConnection createQueueConnection() throws JMSException
    {
        return null;
    }

    public QueueConnection createQueueConnection(String string, String string1) throws JMSException
    {
        return null;
    }
}
