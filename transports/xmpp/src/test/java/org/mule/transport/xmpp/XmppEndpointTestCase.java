/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XmppEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test(expected = InitialisationException.class)
    public void testEndpointWithoutMessageType() throws Exception
    {
        MuleEndpointURI uri = new MuleEndpointURI("xmpp://mule:secret@jabber.org", muleContext);
        uri.initialise();
    }

    @Test
    public void testValidMessageEndpoint() throws Exception
    {
        doTest("xmpp://MESSAGE/mule@jabber.org", "MESSAGE");
    }

    @Test
    public void testValidChatEndpoint() throws Exception
    {
        doTest("xmpp://CHAT/mule@jabber.org", "CHAT");
    }

    @Test
    public void testValidGroupchatEndpoint() throws Exception
    {
        doTest("xmpp://GROUPCHAT/mule@jabber.org", "GROUPCHAT");
    }

    private void doTest(String uriInput, String expectedMessageType) throws Exception
    {
        MuleEndpointURI uri = new MuleEndpointURI(uriInput, muleContext);
        uri.initialise();

        assertEquals("xmpp", uri.getScheme());
        assertEquals(expectedMessageType, uri.getHost());
        assertEquals("/mule@jabber.org", uri.getPath());
    }

    @Test(expected = InitialisationException.class)
    public void testInvalidMessageTypeEndpoint() throws Exception
    {
        MuleEndpointURI uri = new MuleEndpointURI("xmpp://INVALID/mule@jabber.org", muleContext);
        uri.initialise();
    }
}
