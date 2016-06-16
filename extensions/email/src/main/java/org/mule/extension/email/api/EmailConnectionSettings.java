/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Password;

/**
 * A simple POJO with a basic set of parameters required by every
 * email connection.
 *
 * @since 4.0
 */
public class EmailConnectionSettings
{

    /**
     * The host name of the mail server.
     */
    @Parameter
    protected String host;

    /**
     * the username used to connect with the mail server.
     */
    @Parameter
    @Optional
    protected String user;

    /**
     * the password corresponding to the {@code username}.
     */
    @Parameter
    @Password
    @Optional
    protected String password;

    /**
     * @return the host name of the mail server.
     */
    public String getHost()
    {
        return host;
    }

    /**
     * @return the username used to connect with the mail server.
     */
    public String getUser()
    {
        return user;
    }

    /**
     * @return the password corresponding to the {@code username}
     */
    public String getPassword()
    {
        return password;
    }
}
