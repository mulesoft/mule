/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;

public class XmppEndpointTestCase extends AbstractMuleTestCase
{
    public void testEndpointWithoutMessageType() throws Exception
    {
        try
        {
            MuleEndpointURI uri = new MuleEndpointURI("xmpp://mule:secret@jabber.org", muleContext);
            uri.initialise();
            fail("There is no message type set on the endpoint");
        }
        catch (InitialisationException e)
        {
            // expected
        }
    }
    
    public void testValidMessageEndpoint() throws Exception
    {
        doTest("xmpp://MESSAGE/mule@jabber.org", "MESSAGE");
    }
        
    public void testValidChatEndpoint() throws Exception
    {
        doTest("xmpp://CHAT/mule@jabber.org", "CHAT");
    }
    
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

    public void testInvalidMessageTypeEndpoint() throws Exception
    {
        try
        {
            MuleEndpointURI uri = new MuleEndpointURI("xmpp://INVALID/mule@jabber.org", muleContext);
            uri.initialise();
            
            fail("invalid message type not detected");
        }
        catch (InitialisationException e)
        {
            // expected
        }
    }
}
