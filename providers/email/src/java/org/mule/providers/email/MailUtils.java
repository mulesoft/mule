/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.email;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;
import java.util.Properties;

/**
 * Contains javax.mail.Session helpers.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MailUtils
{

    public static Session createMailSession(URLName url)
    {
        if (url == null) {
            throw new IllegalArgumentException("Url argument cannot be null when creating a session");
        }
        Properties props = System.getProperties();
        props.put("mail.smtp.host", url.getHost());
        props.put("mail.smtp.port", String.valueOf(url.getPort()));
        Session session;
        if (url.getPassword() != null) {
            props.put("mail.smtp.auth", "true");
            Authenticator auth = new SMTPAuthenticator(url.getUsername(), url.getPassword());
            session = Session.getInstance(props, auth);
        } else {
            session = Session.getDefaultInstance(props, null);
        }
        return session;
    }

    /**
     * SMTPAuthenticator is used to do simple authentication when the SMTP
     * server requires it.
     */
    private static class SMTPAuthenticator extends javax.mail.Authenticator
    {
        private String username = null;
        private String password = null;

        public SMTPAuthenticator(String user, String pwd) {
            username = user;
            password = pwd;
        }
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
