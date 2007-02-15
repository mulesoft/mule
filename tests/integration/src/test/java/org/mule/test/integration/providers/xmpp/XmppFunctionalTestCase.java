/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.xmpp;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.xmpp.XmppConnector;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import org.jivesoftware.smack.XMPPConnection;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    private XMPPConnection cnn;
    private XmppConnector connector;

    protected void sendTestData(int iterations) throws Exception
    {
        cnn = connector.createXmppConnection(getInDest());
        for (int i = 0; i < 100; i++)
        {
            cnn.createChat("mule1").sendMessage("Test Message:" + i);
        }
    }

    protected void receiveAndTestResults() throws Exception
    {

        Thread.sleep(7000);
        // seem to be getting messages after the test messages??
        assertTrue(callbackCount > 100);
    }

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI("xmpp://mule1:mule@jabber.org.au/foo");
        }
        catch (EndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        try
        {
            return new MuleEndpointURI("xmpp://mule2:mule@jabber.org.au/foobar");
        }
        catch (EndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOConnector createConnector() throws Exception
    {
        connector = new XmppConnector();
        connector.setName("test");
        return connector;
    }

}
