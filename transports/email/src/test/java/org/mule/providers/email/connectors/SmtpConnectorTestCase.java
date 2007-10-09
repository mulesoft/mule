/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.connectors;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.ResponseOutputStream;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.providers.email.MailProperties;
import org.mule.providers.email.SmtpConnector;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

import javax.mail.internet.MimeMessage;

/**
 * Send a message via SMTP to a (greenmail) server.
 */
public class SmtpConnectorTestCase extends AbstractMailConnectorFunctionalTestCase
{    
    public static final long DELIVERY_DELAY_MS = 5000;
    
    public SmtpConnectorTestCase() 
    {
        super(false);
    }

   public UMOConnector createConnector() throws Exception
    {
        SmtpConnector c = new SmtpConnector();
        c.setName("SmtpConnector");
        return c;
    }

    public String getTestEndpointURI()
    {
        return getSmtpTestEndpointURI();
    }

    /**
     * The SmtpConnector does not accept listeners, so the test in the
     * superclass makes no sense here.  Instead, we simply check that
     * a listener is rejected.
     */
    // @Override
    public void testConnectorListenerSupport() throws Exception
    {
        UMOConnector connector = getConnector();
        assertNotNull(connector);

        UMOComponent component = getTestComponent("anApple", Apple.class);
        managementContext.getRegistry().registerComponent(component, managementContext);
        UMOEndpointBuilder builder=new EndpointURIEndpointBuilder(getTestEndpointURI(), managementContext);
        builder.setName("test");
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().createOutboundEndpoint(
            builder, managementContext);
        try
        {
            connector.registerListener(component, endpoint);
            fail("SMTP connector does not accept listeners");
        }
        catch (Exception e)
        {
            assertNotNull("expected", e);
        }
    }

    public void testSend() throws Exception
    {
        repeatTest("doTestSend");
    }

    public void doTestSend() throws Exception
    {
        //managementContext.getRegistry().registerConnector(createConnector(false), managementContext);
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().createOutboundEndpoint(
            getTestEndpointURI(), managementContext);
        
        UMOComponent component = getTestComponent(uniqueName("testComponent"), FunctionalTestComponent.class);
        // TODO Simplify this API for adding an outbound endpoint.
        ((OutboundPassThroughRouter) component.getOutboundRouter().getRouters().get(0)).addEndpoint(endpoint);
        managementContext.getRegistry().registerComponent(component, managementContext);

        UMOMessage message = new MuleMessage(MESSAGE);
        message.setStringProperty(MailProperties.TO_ADDRESSES_PROPERTY, EMAIL);
        UMOSession session = getTestSession(getTestComponent("apple", Apple.class));
        MuleEvent event = new MuleEvent(message, endpoint, session, true, new ResponseOutputStream(System.out));
        endpoint.dispatch(event);

        getServers().waitForIncomingEmail(DELIVERY_DELAY_MS, 1);
        MimeMessage[] messages = getServers().getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
        assertMessageOk(messages[0]);
    }
    
}
