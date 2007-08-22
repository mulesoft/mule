/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import com.icegreen.greenmail.util.Servers;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.user.GreenMailUser;

import java.util.Properties;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.mail.Session;
import javax.mail.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractGreenMailSupport 
{

    public static final String LOCALHOST = "127.0.0.1";
    public static final String MESSAGE = "Test Email Message";
    public static final String AT_EXAMPLE_COM = "@example.com";
    public static final String BOB = "bob";
    public static final String BOB_EMAIL = BOB + AT_EXAMPLE_COM;
    public static final String ALICE = "alice";
    public static final String ALICE_EMAIL = ALICE + AT_EXAMPLE_COM;
    public static final String PASSWORD = "secret";
    public static final long STARTUP_PERIOD_MS = 100;

    protected final Log logger = LogFactory.getLog(this.getClass());
    private Servers servers;

    protected void createUserAndStoreEmail(String email, String user, String password, Object message) throws Exception
    {
        // note that with greenmail 1.1 the Servers object is unreliable
        // and the approach taken in their examples will not work.
        // the following does work, but may break in a later version
        // (there is some confusion in the greenmail code about
        // whether users are identified by email or name alone)
        // in which case try retrieving by EMAIL rather than USER
        logger.debug("Creating mail user " + user + "/" + email + "/" + password);
        UserManager userManager = servers.getManagers().getUserManager();
        userManager.createUser(email, user, password);
        GreenMailUser target = userManager.getUser(user);
        if (null == target)
        {
            throw new IllegalStateException("Failure in greenmail - see comments in test code.");
        }
        target.deliver((MimeMessage) message);
        Thread.sleep(STARTUP_PERIOD_MS);
    }

    protected void createBobAndStoreEmail(Object message) throws Exception
    {
        createUserAndStoreEmail(BOB_EMAIL, BOB, PASSWORD, message);
    }

    protected void createAliceAndStoreEmail(Object message) throws Exception
    {
        createUserAndStoreEmail(ALICE_EMAIL, ALICE, PASSWORD, message);
    }

    protected void startServers() throws Exception
    {
        logger.info("Starting mail servers");
        servers = new Servers(getSetups());
        servers.start();
        Thread.sleep(STARTUP_PERIOD_MS);
    }

    protected abstract int nextPort();

    private ServerSetup[] getSetups()
    {
        return new ServerSetup[]{
                newServerSetup(nextPort(), ServerSetup.PROTOCOL_SMTP),
                newServerSetup(nextPort(), ServerSetup.PROTOCOL_SMTPS),
                newServerSetup(nextPort(), ServerSetup.PROTOCOL_POP3),
                newServerSetup(nextPort(), ServerSetup.PROTOCOL_POP3S),
                newServerSetup(nextPort(), ServerSetup.PROTOCOL_IMAP),
                newServerSetup(nextPort(), ServerSetup.PROTOCOL_IMAPS)
        };
    }

    private ServerSetup newServerSetup(int port, String protocol)
    {
        logger.debug("Server for " + protocol + " will be on port " + port);
        return new ServerSetup(port, null, protocol);
    }

    protected void stopServers() throws Exception
    {
        logger.info("Stopping mail servers");
        if (null != servers)
        {
            servers.stop();
        }
    }

    protected Servers getServers()
    {
        return servers;
    }

    public MimeMessage getValidMessage(String to) throws Exception
    {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setContent(MESSAGE, "text/plain");
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        return message;
    }

}

