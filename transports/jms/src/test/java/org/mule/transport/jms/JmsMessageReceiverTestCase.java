/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsMessageReceiver;

import com.mockobjects.dynamic.AnyConstraintMatcher;
import com.mockobjects.dynamic.Mock;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

public class JmsMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    private JmsConnector connector;

    protected void doSetUp() throws Exception
    {
        connector = new JmsConnector();
        connector.setName("TestConnector");
        connector.setSpecification("1.1");

        Mock connectionFactory = new Mock(ConnectionFactory.class);
        Mock connection = new Mock(Connection.class);
        connectionFactory.expectAndReturn("createConnection", connection.proxy());
        connection.expect("setExceptionListener", new AnyConstraintMatcher());
        connection.expect("close");
        connection.expect("start");
        connection.expect("stop");
        connector.setConnectionFactory((ConnectionFactory) connectionFactory.proxy());
        
        connector.setMuleContext(muleContext);
        //muleContext.applyLifecycle(connector);
        muleContext.getRegistry().registerConnector(connector);
        
        super.doSetUp();
    }

    public void testReceive() throws Exception
    {
        JmsMessageReceiver receiver = (JmsMessageReceiver)getMessageReceiver();
        assertNotNull(receiver.getComponent());
        assertNotNull(receiver.getConnector());
        assertNotNull(receiver.getEndpoint());
        // hmm how do we unit test a message receive
        // receiver.onMessage((Message) getValidMessage());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageReceiverTestCase#getMessageReceiver()
     */
    public MessageReceiver getMessageReceiver() throws Exception
    {
        return new JmsMessageReceiver(endpoint.getConnector(), getTestComponent("orange", Orange.class), endpoint);
    }

    public Object getValidMessage() throws Exception
    {
        return JmsConnectorTestCase.getMessage();
    }

    public ImmutableEndpoint getEndpoint() throws Exception
    {
        EndpointBuilder builder = new EndpointURIEndpointBuilder("jms://testcase", muleContext);
        if (connector == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Connector has not been initialized."), null);
        }
        builder.setConnector(connector);
        endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(builder);
        return endpoint;
    }

}
