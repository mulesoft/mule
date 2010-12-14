/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.email.GreenMailUtilities;
import org.mule.transport.email.ImapConnector;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

public class ImapMessageRequesterTestCase extends DynamicPortTestCase
{
    private static final String EMAIL = "bob@example.com";
    private static final String MESSAGE = "Test email message";
    private static final String PASSWORD = "password";
    private static int PORT = -1;
    private static final String USER = "bob";
    
    private GreenMail server;

    public ImapMessageRequesterTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        PORT = getPorts().get(0);
        startGreenmailServer();
    }

    private void startGreenmailServer() throws Exception
    {
        ServerSetup setup = new ServerSetup(PORT, null, ImapConnector.IMAP);
        server = new GreenMail(setup);
        server.start();
        GreenMailUtilities.storeEmail(server.getManagers().getUserManager(), EMAIL, USER, PASSWORD,
            GreenMailUtilities.toMessage(MESSAGE, EMAIL, null));
    }
    
    @Override
    protected void doTearDown() throws Exception
    {
        server.stop();
        super.doTearDown();
    }

    @Override
    protected String getConfigResources()
    {
        return "imap-message-requester.xml";
    }

    public void testMessageRequester() throws Exception
    {
        muleContext.start();
        String imapUri = String.format("imap://%1s:%2s@localhost:%3d/INBOX", USER, PASSWORD, PORT);
        
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.request(imapUri, RECEIVE_TIMEOUT);
        
        assertNotNull(message);
        assertEquals(MESSAGE, message.getPayload());
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}


