/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.connectors;

import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleEvent;
import org.mule.ResponseOutputStream;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.SmtpConnector;

import com.icegreen.greenmail.util.ServerSetup;

import javax.mail.internet.MimeMessage;

/**
 * Send a message via SMTP to a (greenmail) server.
 */
public class SmtpConnectorTestCase extends AbstractMailConnectorFunctionalTestCase
{    
    public static final long DELIVERY_DELAY_MS = 5000;

    public SmtpConnectorTestCase()
    {
        this(ServerSetup.PROTOCOL_SMTP, 50007);
    } 
    
    public SmtpConnectorTestCase(String protocol, int port)
    {
        super(NO_INITIAL_EMAIL, protocol, port);
    }

    public Connector createConnector() throws Exception
    {
        SmtpConnector c = new SmtpConnector();
        c.setName("SmtpConnector");
        return c;
    }

    /**
     * The SmtpConnector does not accept listeners, so the test in the
     * superclass makes no sense here.  Instead, we simply check that
     * a listener is rejected.
     */
    // @Override
    public void testConnectorListenerSupport() throws Exception
    {
        Connector connector = getConnector();
        assertNotNull(connector);

        Service service = getTestService("anApple", Apple.class);
        //muleContext.getRegistry().registerComponent(service);
        EndpointBuilder builder = new EndpointURIEndpointBuilder(getTestEndpointURI(), muleContext);
        builder.setName("test");
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            builder);
        try
        {
            connector.registerListener(service, endpoint);
            fail("SMTP connector does not accept listeners");
        }
        catch (Exception e)
        {
            assertNotNull("expected", e);
        }
    }

    public void testSend() throws Exception
    {
        //muleContext.getRegistry().registerConnector(createConnector(false));
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            getTestEndpointURI());
        
        Service service = getTestService(uniqueName("testComponent"), FunctionalTestComponent.class);
        // TODO Simplify this API for adding an outbound endpoint.
        ((OutboundPassThroughRouter) service.getOutboundRouter().getRouters().get(0)).addEndpoint(endpoint);
        //muleContext.getRegistry().registerComponent(service);

        MuleMessage message = new DefaultMuleMessage(MESSAGE);
        message.setStringProperty(MailProperties.TO_ADDRESSES_PROPERTY, EMAIL);
        MuleSession session = getTestSession(getTestService("apple", Apple.class));
        DefaultMuleEvent event = new DefaultMuleEvent(message, endpoint, session, true, new ResponseOutputStream(System.out));
        endpoint.dispatch(event);

        getServers().waitForIncomingEmail(DELIVERY_DELAY_MS, 1);
        MimeMessage[] messages = getServers().getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
        assertMessageOk(messages[0]);
    }
    
}
