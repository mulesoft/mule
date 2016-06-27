/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.imap;

import static org.mule.extension.email.internal.EmailProtocol.IMAP;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.IMAP_PORT;
import org.mule.extension.email.api.retriever.AbstractRetrieverProvider;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * A {@link ConnectionProvider} that returns instances of imap based {@link RetrieverConnection}s.
 *
 * @since 4.0
 */
@Alias("imap")
public class IMAPProvider extends AbstractRetrieverProvider<RetrieverConnection>
{

    /**
     * The port number of the mail server.
     */
    @Parameter
    @Optional(defaultValue = IMAP_PORT)
    private String port;

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrieverConnection connect() throws ConnectionException
    {
        return new RetrieverConnection(IMAP,
                                       settings.getUser(),
                                       settings.getPassword(),
                                       settings.getHost(),
                                       port,
                                       getConnectionTimeout(),
                                       getReadTimeout(),
                                       getWriteTimeout(),
                                       getProperties());
    }
}
