/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.imap;

import static org.mule.extension.email.internal.util.EmailConstants.PROTOCOL_IMAP;
import org.mule.extension.email.api.retriever.AbstractRetrieverProvider;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;

/**
 * A {@link ConnectionProvider} that returns instances of imap based {@link RetrieverConnection}s.
 *
 * @since 4.0
 */
@Alias("imap")
public class IMAPProvider extends AbstractRetrieverProvider<IMAPConfiguration, RetrieverConnection>
{

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrieverConnection connect(IMAPConfiguration config) throws ConnectionException
    {
        return new RetrieverConnection(PROTOCOL_IMAP,
                                       user,
                                       password,
                                       config.getHost(),
                                       config.getPort(),
                                       config.getConnectionTimeout(),
                                       config.getReadTimeout(),
                                       config.getWriteTimeout(),
                                       config.getProperties());
    }
}
