/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.xmpp;

import org.jivesoftware.smack.packet.Message;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class XmppConnectorTestCase extends AbstractConnectorTestCase
{
    public UMOConnector getConnector() throws Exception
    {
        XmppConnector cnn = new XmppConnector();
        cnn.setName("xmppConnector");
        cnn.setUsername("mule");
        cnn.setPassword("mule");
        cnn.setServerName("jabber.org.au");
        cnn.initialise();
        return cnn;
    }


    public Object getValidMessage() throws Exception
    {
        return new Message("Hello");
    }

    public String getTestEndpointURI()
    {
        return "xmpp://mule:mule@jabber.org.au";
    }

    public void testConnectorListenerSupport() throws Exception
    {
        //todo fix this. for some reason I can't connect to jabber.org.au
    }
}
