/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.email.GreenMailUtilities;
import org.mule.transport.email.ImapConnector;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import org.junit.Rule;
import org.junit.Test;

public class ImapMessageRequesterTestCase extends FunctionalTestCase
{
    private static final String EMAIL = "bob@example.com";
    private static final String MESSAGE = "Test email message";
    private static final String PASSWORD = "password";
    private static int PORT = -1;
    private static final String USER = "bob";

    private GreenMail server;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");


    @Override
    protected String getConfigFile()
    {
        return "imap-message-requester.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        PORT = dynamicPort.getNumber();
        startGreenmailServer();
    }

    private void startGreenmailServer() throws Exception
    {
        ServerSetup setup = new ServerSetup(PORT, "localhost", ImapConnector.IMAP);
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

    @Test
    public void testMessageRequester() throws Exception
    {
        String imapUri = String.format("imap://%1s:%2s@localhost:%3d/INBOX", USER, PASSWORD, PORT);

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request(imapUri, RECEIVE_TIMEOUT);

        assertNotNull(message);
        assertEquals(MESSAGE, message.getPayload());
    }
}
