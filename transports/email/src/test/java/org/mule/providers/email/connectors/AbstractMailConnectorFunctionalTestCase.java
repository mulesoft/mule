/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.connectors;

import org.mule.providers.email.GreenMailUtilities;
import org.mule.tck.providers.AbstractConnectorTestCase;

import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Servers;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * Start a (greenmail) mail server with a known message, for use in subclasses.
 */
public abstract class AbstractMailConnectorFunctionalTestCase extends AbstractConnectorTestCase
{

    public static final String LOCALHOST = "127.0.0.1";
    public static final String USER = "bob";
    public static final String PROVIDER = "example.com";
    public static final String EMAIL = USER + "@" + PROVIDER;
    public static final String PASSWORD = "secret";
    public static final String MESSAGE = "Test Email Message";

    public static final int START_ATTEMPTS = 3;
    public static final int TEST_ATTEMPTS = 5;
    public static final long STARTUP_PERIOD_MS = 1000;

    // for constructor
    public static final boolean SEND_INITIAL_EMAIL = true;
    public static final boolean NO_INITIAL_EMAIL = false;

    private static final AtomicInteger nameCount = new AtomicInteger(0);

    private MimeMessage message;
    private Servers servers;
    private boolean initialEmail = false;
    private String protocol;
    private int port;

    protected AbstractMailConnectorFunctionalTestCase(boolean initialEmail, String protocol, int port)
    {
        super();
        this.initialEmail = initialEmail;
        this.protocol = protocol;
        this.port = port;
    }
    
    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        startServers();
    }
    
    // @Override
    protected void doTearDown() throws Exception 
    {
        stopServers();
        super.doTearDown();
    }

    private void storeEmail() throws Exception
    {
        GreenMailUtilities.storeEmail(servers.getManagers().getUserManager(),
                EMAIL, USER, PASSWORD, (MimeMessage) getValidMessage());
        assertEquals(1, servers.getReceivedMessages().length);
    }
    
    private void startServers() throws Exception
    {
        servers = new Servers(getSetups());
        GreenMailUtilities.robustStartup(servers, LOCALHOST, port, START_ATTEMPTS, TEST_ATTEMPTS, STARTUP_PERIOD_MS);
        if (initialEmail)
        {
            storeEmail();
        }
    }

    private ServerSetup[] getSetups()
    {
        return new ServerSetup[]{new ServerSetup(port, null, protocol)};
    }

    private void stopServers() throws Exception
    {
        if (null != servers)
        {
            servers.stop();
        }
    }

    protected Servers getServers()
    {
        return servers;
    }

    // @Override
    public Object getValidMessage() throws Exception
    {
        if (null == message)
        {
            message = GreenMailUtilities.toMessage(MESSAGE, EMAIL);
        }
        return message;
    }
    
    public String getTestEndpointURI()
    {
        return protocol + "://" + USER + ":" + PASSWORD + "@" + LOCALHOST + ":" + port +
                "?connector=" + connectorName;
    }

    protected void assertMessageOk(Object message) throws Exception
    {
        assertTrue("Did not receive a MimeMessage", message instanceof MimeMessage);
        MimeMessage received = (MimeMessage) message;
        // for some reason, something is adding a newline at the end of messages
        // so we need to strip that out for comparison
        assertTrue("Did not receive a message with String contents",
            received.getContent() instanceof String);
        String receivedText = ((String) received.getContent()).trim();
        assertEquals(MESSAGE, receivedText);
        assertNotNull(received.getRecipients(Message.RecipientType.TO));
        assertEquals(1, received.getRecipients(Message.RecipientType.TO).length);
        assertEquals(received.getRecipients(Message.RecipientType.TO)[0].toString(), EMAIL);
    }

    protected String uniqueName(String root)
    {
        return root + nameCount.incrementAndGet();
    }

}
