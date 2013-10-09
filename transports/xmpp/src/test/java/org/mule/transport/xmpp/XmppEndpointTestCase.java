/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
