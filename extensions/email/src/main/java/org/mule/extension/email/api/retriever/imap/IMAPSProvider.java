/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.imap;

import static org.mule.extension.email.internal.util.EmailConstants.PROTOCOL_IMAP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;

/**
 * A {@link ConnectionProvider} that returns instances of imaps (secure) based {@link RetrieverConnection}s.
 * <p>
 * The returned connection is secured by TLS.
 *
 * @since 4.0
 */
@Alias("imaps")
public class IMAPSProvider extends IMAPProvider implements Initialisable
{
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
                                       config.getProperties(),
                                       tlsContextFactory);
    }
}
