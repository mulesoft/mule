/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.connectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.ResponseOutputStream;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transport.email.AbstractMailConnector;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.SmtpConnector;
import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import com.icegreen.greenmail.util.ServerSetup;

import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

/**
 * Send a message via SMTP to a (greenmail) server.
 */
public class SmtpConnectorTestCase extends AbstractMailConnectorFunctionalTestCase
{    
    public SmtpConnectorTestCase()
    {
        this(NO_INITIAL_EMAIL, ServerSetup.PROTOCOL_SMTP);
        setStartContext(true);
    } 
    
    protected SmtpConnectorTestCase(boolean initialEmail, String protocol)
    {
        super(initialEmail, protocol);
    }
    
    @Override
    public Connector createConnector() throws Exception
    {
        SmtpConnector c = new SmtpConnector(muleContext);
        c.setName("SmtpConnector");
        return c;
    }
    
    @Override
    protected boolean transformInboundMessage()
    {
        return true;
    }

    /**
     * The SmtpConnector does not accept listeners, so the test in the superclass makes no sense 
     * here. The SMTP transport does not even support inbound endpoints, as SMTP is an outbound
     * transport only so you cannot even create an inbound endpoint to register as listener.
     */
    @Override
    public void testConnectorListenerSupport() throws Exception
    {
        // do nothing
    }

    @Test
    public void testSmtpDoesNotSupportOutboundEndpoints() throws MuleException
    {
        EndpointBuilder builder = new EndpointURIEndpointBuilder(getTestEndpointURI(), muleContext);
        builder.setName("test");

        try
        {
            muleContext.getEndpointFactory().getInboundEndpoint(builder);
            fail("Inbound SMTP endpoints are not supported");
        }
        catch (EndpointException ex)
        {
            // expected
        }
    }

    @Test
    public void testSend() throws Exception
    {
        //muleContext.getRegistry().registerConnector(createConnector(false));
        OutboundEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
            getTestEndpointURI());
        
        Service service = getTestService(uniqueName("testComponent"), FunctionalTestComponent.class);
        // TODO Simplify this API for adding an outbound endpoint.
        OutboundPassThroughRouter passThroughRouter = new OutboundPassThroughRouter();
        passThroughRouter.addRoute(endpoint);
        ((OutboundRouterCollection) service.getOutboundMessageProcessor()).addRoute(passThroughRouter);
        //muleContext.getRegistry().registerComponent(service);

        MuleMessage message = new DefaultMuleMessage(MESSAGE, muleContext);
        message.setOutboundProperty(MailProperties.TO_ADDRESSES_PROPERTY, EMAIL);
        MuleSession session = getTestSession(null, muleContext);
        DefaultMuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            getTestService("apple", Apple.class), session, new ResponseOutputStream(System.out));
        endpoint.process(event);

        getServers().waitForIncomingEmail(AbstractEmailFunctionalTestCase.DELIVERY_DELAY_MS, 1);
        MimeMessage[] messages = getServers().getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
        assertMessageOk(messages[0]);
    }


    // MULE-2130 (Impossible to re-initialise SMTP connector)
    @Test
    public void testConnectorRestart() throws Exception
    {
        Connector c = getConnector();
        assertTrue(c.isStarted());

        c.stop();
        assertFalse(c.isStarted());

        assertFalse(c.isStarted());

        c.start();
        assertFalse(c.isDisposed());
        assertTrue(c.isStarted());
    }

    @Test
    public void testNullUsernameAndPassword() throws Exception
    {
        OutboundEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint("smtp://localhost:23");
        URLName name = ((AbstractMailConnector)getConnector()).urlFromEndpoint(endpoint);
        assertNull(name.getUsername());
        assertNull(name.getPassword());

        endpoint = muleContext.getEndpointFactory().getOutboundEndpoint("smtp://george@localhost:23");
        name = ((AbstractMailConnector)getConnector()).urlFromEndpoint(endpoint);
        assertEquals("george", name.getUsername());
        assertNull(name.getPassword());
    }
}
