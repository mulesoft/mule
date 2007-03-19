/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.providers.email;

import org.mule.tck.providers.AbstractConnectorTestCase;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Servers;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Start a (greenmail) mail server with a known message, for use in subclasses.
 * Each test gets a new set of ports to avoid conflicts (shouldn't be needed, but
 * greenmail doesn't seem to be closing ports).  Also contains utility methods
 * for comparing emails, building endpoints, etc.
 */
public abstract class AbstractMailConnectorFunctionalTestCase extends AbstractConnectorTestCase
{

    public static final int INITIAL_SERVER_PORT = 50001;
    public static final String LOCALHOST = "127.0.0.1";
    public static final String USER = "bob";
    public static final String PROVIDER = "example.com";
    public static final String EMAIL = USER + "@" + PROVIDER;
    public static final String PASSWORD = "secret";
    public static final String MESSAGE = "Test Email Message";
    public static final long STARTUP_PERIOD_MS = 100;
    
    private static final AtomicInteger nextPort = new AtomicInteger(INITIAL_SERVER_PORT);
    private static final Log staticLogger = LogFactory.getLog(AbstractMailConnectorFunctionalTestCase.class);

    private MimeMessage message;
    private Servers servers;
    private boolean initialEmail = false;
    
    protected AbstractMailConnectorFunctionalTestCase(boolean initialEmail)
    {
        this.initialEmail = initialEmail;
    }
    
    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        startServers();
        if (initialEmail)
        {
            storeEmail();
        }
    }
    
    // @Override
    protected void doTearDown() throws Exception 
    {
        stopServers();
        super.doTearDown();
    }

    private void storeEmail() throws Exception
    {
        // note that with greenmail 1.1 the Servers object is unreliable
        // and the approach taken in their examples will not work.
        // the following does work, but may break in a later version
        // (there is some confusion in the greenmail code about
        // whether users are identified by email or name alone)
        // in which case try retrieving by EMAIL rather than USER
        UserManager userManager = servers.getManagers().getUserManager();
        userManager.createUser(EMAIL, USER, PASSWORD);
        GreenMailUser bob = userManager.getUser(USER);
        assertNotNull("Failure in greenmail - see comments in test code.", bob);
        bob.deliver((MimeMessage) getValidMessage());
        assertEquals(1, servers.getReceivedMessages().length);
    }
    
    private void startServers() throws Exception
    {
        servers = new Servers(getSetups());
        servers.start();
    }

    private static ServerSetup[] getSetups()
    {
        staticLogger.debug("generating news servers from: " + nextPort.get());
        ServerSetup smtp = new ServerSetup(nextPort.incrementAndGet(), null, ServerSetup.PROTOCOL_SMTP);
        ServerSetup smtps = new ServerSetup(nextPort.incrementAndGet(), null, ServerSetup.PROTOCOL_SMTPS);
        ServerSetup pop3 = new ServerSetup(nextPort.incrementAndGet(), null, ServerSetup.PROTOCOL_POP3);
        ServerSetup pop3s = new ServerSetup(nextPort.incrementAndGet(), null, ServerSetup.PROTOCOL_POP3S);
        ServerSetup imap = new ServerSetup(nextPort.incrementAndGet(), null, ServerSetup.PROTOCOL_IMAP);
        ServerSetup imaps = new ServerSetup(nextPort.incrementAndGet(), null, ServerSetup.PROTOCOL_IMAPS);
        return new ServerSetup[]{smtp, smtps, pop3, pop3s, imap, imaps};
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
            message = new MimeMessage(Session.getDefaultInstance(new Properties()));
            message.setContent(MESSAGE, "text/plain");
        }
        return message;
    }
    
    protected String getPop3TestEndpointURI()
    {
        return buildEndpoint("pop3", servers.getPop3().getPort());
    }

    protected String getPop3sTestEndpointURI()
    {
        return buildEndpoint("pop3s", servers.getPop3s().getPort());
    }

    protected String getImapTestEndpointURI()
    {
        return buildEndpoint("imap", servers.getImap().getPort());
    }
    
    protected String getImapsTestEndpointURI()
    {
        return buildEndpoint("imaps", servers.getImaps().getPort());
    }
    
    protected String getSmtpTestEndpointURI()
    {
        return buildEndpoint("smtp", servers.getSmtp().getPort());
    }
    
    protected String getSmtpsTestEndpointURI()
    {
        return buildEndpoint("smtps", servers.getSmtps().getPort());
    }
    
   private static String buildEndpoint(String protocol, int port) 
    {
        return protocol + "://" + USER + ":" + PASSWORD + "@" + LOCALHOST + ":" + port;
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
    }

}
