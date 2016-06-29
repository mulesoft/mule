/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.imap;

import static org.mule.extension.email.internal.EmailProtocol.IMAPS;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.IMAPS_PORT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.extension.email.api.retriever.AbstractRetrieverProvider;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * A {@link ConnectionProvider} that returns instances of imaps (secure) based {@link RetrieverConnection}s.
 * <p>
 * The returned connection is secured by TLS.
 *
 * @since 4.0
 */
@Alias("imaps")
public class IMAPSProvider extends AbstractRetrieverProvider<RetrieverConnection> implements Initialisable
{

    /**
     * The port number of the mail server.
     */
    @Parameter
    @Optional(defaultValue = IMAPS_PORT)
    private String port;

    /**
     * A factory for TLS contexts. A TLS context is configured with a key store and a trust store.
     * Allows to create a TLS secured connections.
     */
    @Parameter
    private TlsContextFactory tlsContextFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise() throws InitialisationException
    {
        initialiseIfNeeded(tlsContextFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrieverConnection connect() throws ConnectionException
    {
        return new RetrieverConnection(IMAPS,
                                       settings.getUser(),
                                       settings.getPassword(),
                                       settings.getHost(),
                                       port,
                                       getConnectionTimeout(),
                                       getReadTimeout(),
                                       getWriteTimeout(),
                                       getProperties(),
                                       tlsContextFactory);
    }
}
