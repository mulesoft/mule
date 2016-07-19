/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import javax.inject.Inject;

/**
 * Configuration for operations that are performed through the SMTP (Simple Mail Transfer Protocol) protocol.
 *
 * @since 4.0
 */
@Operations(SenderOperations.class)
@Providers({SMTPProvider.class, SMTPSProvider.class})
@Configuration(name = "smtp")
@DisplayName("SMTP")
@Summary("Configuration for operations that are performed through the SMTP protocol")
public class SMTPConfiguration implements Initialisable
{

    @Inject
    private MuleContext muleContext;

    /**
     * The from address. The person that is going to send the messages.
     */
    @Parameter
    @Optional
    @Summary("The \"From\" sender address.")
    @Placement(group = "General")
    private String from;

    /**
     * Default character encoding to be used in all the messages. If not specified, the default charset in the mule
     * configuration will be used
     */
    @Parameter
    @Optional
    @Summary("Default character encoding to be used in all the messages. If not specified, the default charset in " +
             "the mule configuration will be used")
    @Placement(group = "Advance")
    private String defaultCharset;

    /**
     * @return the address of the person that is going to send the messages.
     */
    public String getFrom()
    {
        return from;
    }

    public String getDefaultCharset()
    {
        return defaultCharset;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (defaultCharset == null)
        {
            defaultCharset = muleContext.getConfiguration().getDefaultEncoding();
        }
    }
}
