/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.retriever.imap;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import org.mule.extension.email.internal.retriever.RetrieverConfiguration;
import org.mule.extension.email.internal.retriever.RetrieverOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Configuration for operations that are performed through the IMAP (Internet Message Access Protocol) protocol.
 *
 * @since 4.0
 */
@Operations({IMAPOperations.class, RetrieverOperations.class})
@Providers({IMAPProvider.class, IMAPSProvider.class})
@Configuration(name = "imap")
@DisplayName("IMAP")
public class IMAPConfiguration implements RetrieverConfiguration
{

    /**
     * Default encoding to be used in all the messages. If not specified, it defaults
     * to the default encoding in the mule configuration
     */
    @Parameter
    @Optional
    @Placement(group = ADVANCED)
    private String defaultCharset;
    /**
     * Indicates whether the retrieved emails should be opened and read. The default value is "true".
     */
    @Parameter
    @Optional(defaultValue = "true")
    private boolean eagerlyFetchContent;

    public String getDefaultCharset()
    {
        return defaultCharset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEagerlyFetchContent()
    {
        return eagerlyFetchContent;
    }

}
