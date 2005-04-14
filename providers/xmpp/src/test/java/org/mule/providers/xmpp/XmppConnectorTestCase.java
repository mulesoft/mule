/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
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
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppConnectorTestCase extends AbstractConnectorTestCase
{
    public UMOConnector getConnector() throws Exception
    {
        XmppConnector cnn = new XmppConnector();
        cnn.setName("xmppConnector");
        cnn.initialise();
        return cnn;
    }


    public Object getValidMessage() throws Exception
    {
        return new Message("Hello");
    }

    public String getTestEndpointURI()
    {
        return "xmpp://mule1:mule@jabber.org.au";
    }
}
