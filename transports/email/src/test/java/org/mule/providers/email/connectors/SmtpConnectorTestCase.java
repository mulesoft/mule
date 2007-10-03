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

import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.ResponseOutputStream;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.email.MailProperties;
import org.mule.providers.email.SmtpConnector;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;

import java.util.HashMap;

import javax.mail.internet.MimeMessage;

/**
 * Send a message via SMTP to a (greenmail) server.
 */
public class SmtpConnectorTestCase extends AbstractMailConnectorFunctionalTestCase
{
    
    public static final long DELIVERY_DELAY_MS = 5000;
    
    public SmtpConnectorTestCase() 
    {
        this("SmtpConnector");
    }

    SmtpConnectorTestCase(String connectorName) 
    {
        super(false, connectorName);
    }

   public UMOConnector createConnector(boolean init) throws Exception
    {
        SmtpConnector c = new SmtpConnector();
        c.setName(getConnectorName());
        if (init)
        {
            c.initialise();
        }
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

        UMOModel model = getModel();
        MuleDescriptor d = getTestDescriptor("anApple", Apple.class.getName());
        d.setModelName(model.getName());

        managementContext.getRegistry().registerService(d);
        UMOComponent component = model.getComponent(d.getName());
        UMOEndpointBuilder builder=new EndpointURIEndpointBuilder(new MuleEndpointURI(getTestEndpointURI()), managementContext);
        builder.setName("test");
        UMOImmutableEndpoint endpoint = builder.buildOutboundEndpoint();
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
        HashMap props = new HashMap();

        managementContext.getRegistry().registerConnector(createConnector(false), managementContext);
        UMOEndpoint endpoint = new MuleEndpoint(getTestEndpointURI(), false);
        managementContext.getRegistry().registerService(
            MuleTestUtils.createDescriptor(FunctionalTestComponent.class.getName(), 
                                           uniqueName("testComponent"), null, endpoint, props),
            managementContext);

        UMOMessage message = new MuleMessage(MESSAGE);
        message.setStringProperty(MailProperties.TO_ADDRESSES_PROPERTY, EMAIL);
        UMOSession session = 
            getTestSession(getTestComponent(getTestDescriptor("apple", Apple.class.getName())));
        MuleEvent event = 
            new MuleEvent(message, endpoint, session, true, new ResponseOutputStream(System.out));
        endpoint.dispatch(event);

        getServers().waitForIncomingEmail(DELIVERY_DELAY_MS, 1);
        MimeMessage[] messages = getServers().getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
        assertMessageOk(messages[0]);
    }
    
}
