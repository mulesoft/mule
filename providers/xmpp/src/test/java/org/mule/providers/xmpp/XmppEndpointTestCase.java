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

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppEndpointTestCase extends NamedTestCase
{
    public void testXmppUrl() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI("xmpp://mule:secret@jabber.org");
        assertEquals("xmpp", endpointUri.getScheme());
        assertEquals("mule@jabber.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertEquals("jabber.org", endpointUri.getHost());
        assertEquals("mule:secret", endpointUri.getUserInfo());
        assertEquals("xmpp://mule:secret@jabber.org", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
    }

    public void testXmppUrlWithPortAndParam() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI("xmpp://mule:secret@jabber.org:6666?chatGroup=mule");
        assertEquals("xmpp", endpointUri.getScheme());
        assertEquals("mule@jabber.org:6666", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(6666, endpointUri.getPort());
        assertEquals("jabber.org", endpointUri.getHost());
        assertEquals("mule:secret", endpointUri.getUserInfo());
        assertEquals("xmpp://mule:secret@jabber.org:6666?chatGroup=mule", endpointUri.toString());
        assertEquals(1, endpointUri.getParams().size());
        assertEquals("mule", endpointUri.getParams().get("chatGroup"));
    }
}
