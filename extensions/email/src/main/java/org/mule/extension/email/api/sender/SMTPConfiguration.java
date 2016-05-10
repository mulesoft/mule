/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.sender;

import static org.mule.extension.email.internal.util.EmailConstants.PORT_SMTP;
import org.mule.extension.email.api.AbstractEmailConfiguration;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * Configuration for operations that are performed through the SMTP
 * protocol.
 *
 * @since 4.0
 */
@Operations(SenderOperations.class)
@Providers({SMTPProvider.class, SMTPSProvider.class})
@Configuration(name = "smtp")
public class SMTPConfiguration extends AbstractEmailConfiguration
{

    /**
     * the port number of the mail server.
     */
    @Parameter
    @Optional(defaultValue = PORT_SMTP)
    protected String port;


    /**
     * The from address. The person that is going to send the messages.
     */
    @Parameter
    @Optional
    private String from;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPort()
    {
        return port;
    }

    /**
     * @return the address of the person that is going to send the messages.
     */
    public String getFrom()
    {
        return from;
    }
}
