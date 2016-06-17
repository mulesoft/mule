/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.pop3;

import org.mule.extension.email.api.AbstractEmailConfiguration;
import org.mule.extension.email.api.retriever.RetrieverConfiguration;
import org.mule.extension.email.api.retriever.RetrieverOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connector.Providers;

/**
 * Configuration for operations that are performed through the POP3
 * protocol.
 *
 * @since 4.0
 */
@Operations(RetrieverOperations.class)
@Providers({POP3Provider.class, POP3SProvider.class})
@Configuration(name = "pop3")
public class POP3Configuration extends AbstractEmailConfiguration implements RetrieverConfiguration
{

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
