/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.providers.email;

import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.ResponseOutputStream;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.email.MailProperties;
import org.mule.providers.email.SmtpConnector;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;

import java.util.HashMap;

import javax.mail.internet.MimeMessage;

/**
 * Send a message via SMTP to a (greenmail) server.
 */
public class SmtpConnectorTestCase extends AbstractMailConnectorFunctionalTestCase
{
    
    /** 
     * Javamail waits 30s before delivering, or getmail spends 30s doing nothing.
     * I have no idea why, but it's exactly 30 seconds every time.
     */
    public static final long DELIVERY_DELAY_MS = 31000;
    
    public SmtpConnectorTestCase() 
    {
        super(false);
    }

    public UMOConnector getConnector() throws Exception
    {
        SmtpConnector c = new SmtpConnector();
        c.setName("SmtpConnector");
        c.initialise();
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
        assertNotNull(connector);
        MuleDescriptor d = getTestDescriptor("anApple", Apple.class.getName());
        UMOComponent component = model.registerComponent(d);
        UMOEndpoint endpoint = 
            new MuleEndpoint("test", new MuleEndpointURI(getTestEndpointURI()), connector,
                null, UMOEndpoint.ENDPOINT_TYPE_SENDER, 0, null, new HashMap());
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
        HashMap props = new HashMap();

        UMOEndpoint endpoint = new MuleEndpoint(getTestEndpointURI(), false);
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        builder.registerComponent(FunctionalTestComponent.class.getName(), 
            "testComponent", null, endpoint, props);

        logger.debug("starting mule");
        managementContext.start();

        // default transformer is string to mail message, so send string
        UMOMessage message = new MuleMessage(MESSAGE);
        message.setStringProperty(MailProperties.TO_ADDRESSES_PROPERTY, EMAIL);
        UMOSession session = 
            getTestSession(getTestComponent(getTestDescriptor("apple", Apple.class.getName())));
        MuleEvent event = 
            new MuleEvent(message, endpoint, session, true, new ResponseOutputStream(System.out));
        endpoint.dispatch(event);

        getServers().waitForIncomingEmail(DELIVERY_DELAY_MS, 1);
        MimeMessage[] messages = getServers().getReceivedMessages();
        int count = null == messages ? 0 : messages.length;
        assertEquals("did not receive mail", 1, count);
        assertMessageOk(messages[0]);
    }
    
}
