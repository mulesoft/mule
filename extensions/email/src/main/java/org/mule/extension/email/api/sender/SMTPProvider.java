/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.sender;

import static org.mule.extension.email.internal.EmailProtocol.SMTP;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.SMTP_PORT;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * A {@link ConnectionProvider} that returns instances of smtp based {@link SenderConnection}s.
 *
 * @since 4.0
 */
@Alias("smtp")
public class SMTPProvider extends AbstractSenderProvider
{
    /**
     * The port number of the mail server.
     */
    @Parameter
    @Optional(defaultValue = SMTP_PORT)
    private String port;

    /**
     * {@inheritDoc}
     */
    @Override
    public SenderConnection connect(SMTPConfiguration config) throws ConnectionException
    {
        return new SenderConnection(SMTP,
                                    settings.getUser(),
                                    settings.getPassword(),
                                    settings.getHost(),
                                    port,
                                    config.getConnectionTimeout(),
                                    config.getReadTimeout(),
                                    config.getWriteTimeout(),
                                    config.getProperties());
    }
}
