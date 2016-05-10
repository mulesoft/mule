/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.imap;

import static org.mule.extension.email.internal.util.EmailConstants.DEFAULT_FOLDER;
import static org.mule.extension.email.internal.util.EmailConstants.PORT_IMAP;
import org.mule.extension.email.api.AbstractEmailConfiguration;
import org.mule.extension.email.api.retriever.RetrieverConfiguration;
import org.mule.extension.email.api.retriever.RetrieverOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 *  Configuration for operations that are performed through the IMAP
 *  protocol.
 *
 * @since 4.0
 */
@Operations({IMAPOperations.class, RetrieverOperations.class})
@Providers({IMAPProvider.class, IMAPSProvider.class})
@Configuration(name = "imap")
public class IMAPConfiguration extends AbstractEmailConfiguration implements RetrieverConfiguration
{

    /**
     * The port number of the mail server. The default value for imap mail servers is 143.
     */
    @Parameter
    @Optional(defaultValue = PORT_IMAP)
    private String port;

    /**
     * The folder from which emails are going to be
     * retrieved. The default one is the "INBOX" folder.
     */
    @Parameter
    @Optional(defaultValue = DEFAULT_FOLDER)
    private String folder;

    /**
     * Indicates whether the retrieved emails should be opened
     * and read. The default value is {@code true}.
     */
    @Parameter
    @Optional(defaultValue = "true")
    private boolean eagerlyFetchContent;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEagerlyFetchContent()
    {
        return eagerlyFetchContent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPort()
    {
        return port;
    }
}
