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

import javax.inject.Inject;

/**
 * Configuration for operations that are performed through the SMTP
 * protocol.
 *
 * @since 4.0
 */
@Operations(SenderOperations.class)
@Providers({SMTPProvider.class, SMTPSProvider.class})
@Configuration(name = "smtp")
public class SMTPConfiguration implements Initialisable
{

    @Inject
    private MuleContext muleContext;

    /**
     * The from address. The person that is going to send the messages.
     */
    @Parameter
    @Optional
    private String from;

    /**
     * Default encoding to be used in all the messages. If not specified, it defaults
     * to the default encoding in the mule configuration
     */
    @Parameter
    @Optional
    private String defaultEncoding;

    /**
     * @return the address of the person that is going to send the messages.
     */
    public String getFrom()
    {
        return from;
    }


    public String getDefaultEncoding()
    {
        return defaultEncoding;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (defaultEncoding == null)
        {
            defaultEncoding = muleContext.getConfiguration().getDefaultEncoding();
        }
    }
}
