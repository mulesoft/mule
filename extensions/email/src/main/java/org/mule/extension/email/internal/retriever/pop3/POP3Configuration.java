/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.retriever.pop3;

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
 * Configuration for operations that are performed through the POP3 (Post Office Protocol 3) protocol.
 *
 * @since 4.0
 */
@Operations(RetrieverOperations.class)
@Providers({POP3Provider.class, POP3SProvider.class})
@Configuration(name = "pop3")
@DisplayName("POP3")
public class POP3Configuration implements RetrieverConfiguration
{

    /**
     * Default encoding to be used in all the messages. If not specified, it defaults
     * to the default encoding in the mule configuration
     */
    @Parameter
    @Optional
    @Placement(group = ADVANCED)
    private String defaultCharset;

    public String getDefaultCharset()
    {
        return defaultCharset;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The pop3 protocol always read the content when retrieves an email.
     */
    @Override
    public boolean isEagerlyFetchContent()
    {
        return true;
    }
}
