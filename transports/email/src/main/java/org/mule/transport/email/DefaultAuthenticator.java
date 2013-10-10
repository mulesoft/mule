/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email;

import javax.mail.PasswordAuthentication;

/**
 * DefaultAuthenticator is used to do simple authentication when the SMTP server
 * requires it.
 */
class DefaultAuthenticator extends javax.mail.Authenticator
{
    private String username = null;
    private String password = null;

    public DefaultAuthenticator(String user, String pwd)
    {
        username = user;
        password = pwd;
    }

    public PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(username, password);
    }
}
