/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * An {@link Authenticator} object that knows how to obtain
 * authentication for a network connection using username and password.
 *
 * @since 4.0
 */
public final class PasswordAuthenticator extends Authenticator
{

    private String user;
    private String pass;

    /**
     * Creates a new instance.
     *
     * @param user the username to establish connection to.
     * @param pass the password for the specified {@code username}.
     */
    public PasswordAuthenticator(String user, String pass)
    {
        this.user = user;
        this.pass = pass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(user, pass);
    }
}
