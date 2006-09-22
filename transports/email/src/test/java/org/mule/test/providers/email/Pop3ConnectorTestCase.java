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
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.email.Pop3Connector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Pop3ConnectorTestCase extends AbstractConnectorTestCase
{
    private Message message;

    public UMOConnector getConnector() throws Exception
    {
        Pop3Connector c = new Pop3Connector();
        c.setName("Pop3connector");
        c.initialise();
        return c;
    }

    public String getTestEndpointURI()
    {
        return "pop3://a:a@muleumo.com";
    }

    public Object getValidMessage() throws Exception
    {
        if (message == null) {
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
        UMOEndpoint endpoint = new MuleEndpoint(getTestEndpointURI(), true);
        connector.registerListener(component, endpoint);
    }

    public void testConnectorListenerSupport() throws Exception
    {
        UMOConnector connector = getConnector();
        assertNotNull(connector);

        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        UMOComponent component = getTestComponent(d);
        UMOEndpoint endpoint = new MuleEndpoint(getTestEndpointURI(), true);
        endpoint.setConnector(connector);

        try {
            connector.registerListener(null, null);
            fail("cannot register null");
        } catch (Exception e) { /* expected */
        }

        try {
            connector.registerListener(null, endpoint);
            fail("cannot register null");
        } catch (Exception e) { /* expected */
        }

        try {
            connector.registerListener(component, null);
            fail("cannot register null");
        } catch (Exception e) { /* expected */
        }
        connector.dispose();
    }
}
