/*
 * $Id$
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Messages;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Properties;

/**
 * Contains javax.mail.Session helpers.
 *
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MailUtils
{
    /**
     * The logger used for this class
     */
    protected final static transient Log logger = LogFactory.getLog(MailUtils.class);


    /**
     * Creates a new Mail session based on a Url.  this method will also add an Smtp Authenticator
     * if a password is set on the URL
     * @param url
     * @return initialised mail session
     */
    public static Session createMailSession(URLName url, MailConnector connector)
    {
        if (url == null) {
            throw new IllegalArgumentException(new org.mule.config.i18n.Message(Messages.X_IS_NULL, "URL").toString());
        }
        String protocol = connector.getProtocol().toLowerCase();
        boolean secure = false;
        if(protocol.equals("smtps")) {
            protocol = "smtp";
            secure=true;
        } else if(protocol.equals("pop3s")) {
            protocol = "pop3";
            secure = true;
        } else if(protocol.equals("imaps")) {
            protocol = "imap";
            secure = true;
        }

        Properties props = System.getProperties();
        Session session;

        // make sure we do not mess with authentication set via system properties
        synchronized (props) {
            props.put("mail." + protocol +".host", url.getHost());
            int port = url.getPort();
            if(port==-1) {
                port = connector.getDefaultPort();
            }
            props.put("mail." + protocol + ".port", String.valueOf(port));

            if(secure) {
                System.setProperty("mail." + protocol + ".socketFactory.port", String.valueOf(port));

            }
            props.setProperty("mail." + protocol + ".rsetbeforequit","true");
            
            if (url.getPassword() != null) {
                props.put("mail." + protocol + ".auth", "true");
                Authenticator auth = connector.getAuthenticator();
                if(auth==null) {
                    auth = new DefaultAuthenticator(url.getUsername(), url.getPassword());
                    logger.debug("No Authenticator set on Connector: " + connector.getName() + ". Using default.");
                }
                session = Session.getInstance(props, auth);
            } else {
                // reset authentication property so smtp is not affected (MULE-464)
                props.put("mail." + protocol + ".auth", "false");
                session = Session.getDefaultInstance(props, null);
            }
        }

        return session;
    }

    public static String internetAddressesToString(InternetAddress[] addresses) {
        if(addresses==null || addresses.length==0) {
            return StringUtils.EMPTY;
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < addresses.length; i++) {
            InternetAddress address = addresses[i];
            buf.append(address.getAddress());
            // all except the last one
            if (i < addresses.length - 1) {
                buf.append(", ");
            }
        }
        return  buf.toString();
    }

    public static String internetAddressesToString(InternetAddress address) {
        return internetAddressesToString(new InternetAddress[]{address});
    }

    public static String mailAddressesToString(Address[] addresses) {
        if(addresses==null || addresses.length==0) {
            return StringUtils.EMPTY;
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < addresses.length; i++) {
            Address address = addresses[i];
            buf.append(address.toString());
            // all except the last one
            if (i < addresses.length - 1) {
                buf.append(", ");
            }
        }
        return  buf.toString();
    }

    public static String mailAddressesToString(Address address) {
        return mailAddressesToString(new Address[]{address});
    }

    public static InternetAddress[] stringToInternetAddresses(String address) throws AddressException
    {
        InternetAddress[] inetaddresses;
        if (StringUtils.isNotBlank(address)) {
            inetaddresses = InternetAddress.parse(address, false);
        } else {
            throw new NullPointerException(new org.mule.config.i18n.Message(Messages.X_IS_NULL, "Email address").toString());
        }
        return inetaddresses;
    }

    /**
     * DefaultAuthenticator is used to do simple authentication when the SMTP
     * server requires it.
     */
    private static class DefaultAuthenticator extends javax.mail.Authenticator
    {
        private String username = null;
        private String password = null;

        public DefaultAuthenticator(String user, String pwd) {
            username = user;
            password = pwd;
        }
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
