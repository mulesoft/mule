/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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
    private GreenMail servers;

    protected void createUserAndStoreEmail(String email, String user, String password, Object message) throws Exception
    {
        // note that with greenmail 1.1 the Servers object is unreliable
        // and the approach taken in their examples will not work.
        // the following does work, but may break in a later version
        // (there is some confusion in the greenmail code about
        // whether users are identified by email or name alone)
        // in which case try retrieving by EMAIL rather than USER
        logger.debug("Creating mail user " + user + "/" + email + "/" + password);
        GreenMailUser target = createUser(email, user, password);
        target.deliver((MimeMessage) message);
        Thread.sleep(STARTUP_PERIOD_MS);
    }

    public GreenMailUser createUser(String email, String user, String password) throws UserException
    {
        UserManager userManager = servers.getManagers().getUserManager();
        userManager.createUser(email, user, password);
        GreenMailUser target = userManager.getUser(user);
        if (null == target)
        {
            throw new IllegalStateException("Failure in greenmail - see comments in test code.");
        }
        return target;
    }

    public void createBobAndStoreEmail(Object message) throws Exception
    {
        createUserAndStoreEmail(BOB_EMAIL, BOB, PASSWORD, message);
    }

    public void createAliceAndStoreEmail(Object message) throws Exception
    {
        createUserAndStoreEmail(ALICE_EMAIL, ALICE, PASSWORD, message);
    }

    public void startServers(List<Integer> list) throws Exception
    {
        logger.info("Starting mail servers");
        servers = new GreenMail(getSetups(list));
        servers.start();
        Thread.sleep(STARTUP_PERIOD_MS);
    }

    protected abstract int nextPort();

    private ServerSetup[] getSetups(List<Integer> list)
    {
        if (list.size() != 6)
        {
            throw new IllegalArgumentException("must pass in an array of 6 ports for server setup");
        }
        
        return new ServerSetup[]{
                newServerSetup(list.get(0), ServerSetup.PROTOCOL_POP3),
                newServerSetup(list.get(1), ServerSetup.PROTOCOL_SMTP),
                newServerSetup(list.get(2), ServerSetup.PROTOCOL_SMTPS),
                newServerSetup(list.get(3), ServerSetup.PROTOCOL_POP3S),
                newServerSetup(list.get(4), ServerSetup.PROTOCOL_IMAP),
                newServerSetup(list.get(5), ServerSetup.PROTOCOL_IMAPS)
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

    public GreenMail getServers()
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

