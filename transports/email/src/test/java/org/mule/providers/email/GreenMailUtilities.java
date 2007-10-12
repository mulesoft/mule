/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.Servers;

import java.net.Socket;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GreenMailUtilities
{

    protected static Log logger = LogFactory.getLog(GreenMailUtilities.class);

    public static void storeEmail(UserManager userManager, String email, String user, String password,
                                  MimeMessage message)
            throws Exception
    {
        // note that with greenmail 1.1 the Servers object is unreliable
        // and the approach taken in their examples will not work.
        // the following does work, but may break in a later version
        // (there is some confusion in the greenmail code about
        // whether users are identified by email or name alone)
        // in which case try retrieving by EMAIL rather than USER
        userManager.createUser(email, user, password);
        GreenMailUser gmUser = userManager.getUser(user);
        assert null != gmUser;
        gmUser.deliver(message);
    }

    public static MimeMessage toMessage(String text, String email) throws MessagingException
    {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setContent(text, "text/plain");
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        return message;
    }

    public static void waitForStartup(String host, int port, int count, long wait) throws InterruptedException
    {
        for (int i = 0; i < count; ++i)
        {
            Thread.sleep(wait);
            try {
                Socket socket = new Socket(host, port);
                socket.close();
                logger.info("Successful connection made to port " + port);
                return;
            }
            catch (Exception e)
            {
                logger.warn("Could not connect to server on " + host + ":" + port + " - " + e.getMessage());
            }
        }
        throw new RuntimeException("Server failed to start within " + (count * wait) + "ms");
    }

    public static void robustStartup(Servers servers, String host, int port, int startMax, int testMax, long wait)
            throws InterruptedException
    {
        for (int start = 0; start < startMax; ++start)
        {
            try
            {
                servers.start();
                waitForStartup(host, port, testMax, wait);
                return;
            }
            catch (Exception e)
            {
                try
                {
                    servers.stop();
                }
                catch (Throwable t)
                {
                    // ignore
                }
            }
            Thread.sleep(wait);
        }
    }

}
