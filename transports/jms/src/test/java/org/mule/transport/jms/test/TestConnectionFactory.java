/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.test;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

public class TestConnectionFactory implements QueueConnectionFactory
{
    private String providerProperty = "NOT_SET";
    private String connectionFactoryProperty = "NOT_SET";
    private Object customProperty;

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
    
    public Object getCustomProperty()
    {
        return customProperty;
    }
    
    public void setCustomProperty(Object custom)
    {
        customProperty = custom;
    }
}
