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

import org.mule.impl.MuleDescriptor;
import org.mule.providers.email.Pop3Connector;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.UMOComponent;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.Properties;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

/**
 * Simple tests for pulling from a POP3 server.
 */
public class Pop3ConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{

    private Message message;

    public UMOConnector getConnector() throws Exception
    {
        Pop3Connector connector = new Pop3Connector();
        connector.setName("Pop3Connector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.initialise();
        return connector;
    }

    public String getTestEndpointURI()
    {
        return getPop3TestEndpointURI();
    }

    public Object getValidMessage() throws Exception
    {
        if (message == null)
        {
            message = new MimeMessage(Session.getDefaultInstance(new Properties()));
            message.setContent("Test Email Message", "text/plain");
        }
        return message;
    }

    public void testReceiver() throws Exception
    {
        UMOConnector connector = getConnector();
        assertNotNull(connector);
        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        UMOComponent component = getTestComponent(d);
        UMOEndpoint endpoint = managementContext.getRegistry().getOrCreateEndpointForUri(getTestEndpointURI(), UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        connector.registerListener(component, endpoint);
    }

    public void testConnectorListenerSupport() throws Exception
    {
        UMOConnector connector = getConnector();
        assertNotNull(connector);

        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        UMOComponent component = getTestComponent(d);
        UMOEndpoint endpoint = managementContext.getRegistry().getOrCreateEndpointForUri(getTestEndpointURI(), UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        endpoint.setConnector(connector);

        try
        {
            connector.registerListener(null, null);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(null, endpoint);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(component, null);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        connector.dispose();
    }
}
