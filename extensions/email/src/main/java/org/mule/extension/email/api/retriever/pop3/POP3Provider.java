/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.pop3;

import static org.mule.extension.email.internal.EmailProtocol.POP3;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.POP3_PORT;
import org.mule.extension.email.api.retriever.AbstractRetrieverProvider;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * A {@link ConnectionProvider} that returns instances of pop3 based {@link RetrieverConnection}s.
 *
 * @since 4.0
 */
@Alias("pop3")
public class POP3Provider extends AbstractRetrieverProvider<POP3Configuration, RetrieverConnection>
{
    /**
     * The port number of the mail server.
     */
    @Parameter
    @Optional(defaultValue = POP3_PORT)
    private String port;

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrieverConnection connect(POP3Configuration config) throws ConnectionException
    {
        return new RetrieverConnection(POP3,
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
