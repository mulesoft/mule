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
