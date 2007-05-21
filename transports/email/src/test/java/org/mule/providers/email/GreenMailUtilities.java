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

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class GreenMailUtilities
{

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

}
