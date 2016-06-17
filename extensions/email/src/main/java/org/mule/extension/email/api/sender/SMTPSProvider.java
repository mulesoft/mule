/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.sender;

import static org.mule.extension.email.internal.EmailProtocol.SMTPS;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.SMTPS_PORT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
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
 * A {@link ConnectionProvider} that returns instances of smtps based {@link RetrieverConnection}s.
 * <p>
 * The returned connection is secured by TLS.
 *
 * @since 4.0
 */
@Alias("smtps")
public class SMTPSProvider extends AbstractSenderProvider implements Initialisable
{
    /**
     * The port number of the mail server.
     */
    @Parameter
    @Optional(defaultValue = SMTPS_PORT)
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
    public SenderConnection connect(SMTPConfiguration config) throws ConnectionException
    {
        return new SenderConnection(SMTPS,
                                    settings.getUser(),
                                    settings.getPassword(),
                                    settings.getHost(),
                                    port,
                                    config.getConnectionTimeout(),
                                    config.getReadTimeout(),
                                    config.getWriteTimeout(),
                                    config.getProperties(),
                                    tlsContextFactory);
    }
}
