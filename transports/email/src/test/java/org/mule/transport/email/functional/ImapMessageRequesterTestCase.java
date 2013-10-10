/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.email.GreenMailUtilities;
import org.mule.transport.email.ImapConnector;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    protected String getConfigResources()
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
        
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.request(imapUri, RECEIVE_TIMEOUT);
        
        assertNotNull(message);
        assertEquals(MESSAGE, message.getPayload());
    }

}


