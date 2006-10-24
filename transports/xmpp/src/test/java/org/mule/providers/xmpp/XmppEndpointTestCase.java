/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.MalformedEndpointException;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppEndpointTestCase extends AbstractMuleTestCase
{
    public void testMalformedXmppUrl() throws Exception
    {
        try
        {
            new MuleEndpointURI("xmpp://mule:secret@jabber.org");
            fail("There is no path set on the endpoint");
        }
        catch (MalformedEndpointException e)
        {
            // expected
        }
    }

    public void testXmppUrlWithPortAndToChat() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI(
            "xmpp://mule:secret@jabber.org:6666/ross@jabber.org");
        assertEquals("xmpp", endpointUri.getScheme());
        assertEquals("mule@jabber.org:6666", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(6666, endpointUri.getPort());
        assertEquals("jabber.org", endpointUri.getHost());
        assertEquals("mule:secret", endpointUri.getUserInfo());
        assertEquals("xmpp://mule:secret@jabber.org:6666/ross@jabber.org", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("ross@jabber.org", endpointUri.getPath().substring(1));
    }

    public void testXmppUrlWithPortAndToChatWithParam() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI(
            "xmpp://mule:secret@jabber.org:6666/ross@jabber.org?groupChat=true&nickname=ross");
        assertEquals("xmpp", endpointUri.getScheme());
        assertEquals("mule@jabber.org:6666", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(6666, endpointUri.getPort());
        assertEquals("jabber.org", endpointUri.getHost());
        assertEquals("mule:secret", endpointUri.getUserInfo());
        assertEquals("xmpp://mule:secret@jabber.org:6666/ross@jabber.org?groupChat=true&nickname=ross",
            endpointUri.toString());
        assertEquals(2, endpointUri.getParams().size());
        assertEquals("ross@jabber.org", endpointUri.getPath().substring(1));
        assertEquals("true", endpointUri.getParams().get("groupChat"));
        assertEquals("ross", endpointUri.getParams().get("nickname"));
    }

    public void testXmppBadGroupChatParams() throws Exception
    {
        try
        {
            new MuleEndpointURI("xmpp://mule:secret@jabber.org:6666/ross@jabber.org?groupChat=true");
            fail("if groupchat is set to true a nickname must be set");
        }
        catch (MalformedEndpointException e)
        {
            // expected
        }

        new MuleEndpointURI("xmpp://mule:secret@jabber.org:6666/ross@jabber.org?groupChat=false");
    }
}
