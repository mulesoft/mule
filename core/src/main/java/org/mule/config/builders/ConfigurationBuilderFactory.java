/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.config.ConfigResource;

import java.util.List;

/**
 * Creates {@link ConfigurationBuilder}
 */
public interface ConfigurationBuilderFactory
{

    /**
     * Creates a configuration builder for an application's context
     *
     * @param domainContext context for application domain
     * @param configs application configuration resources
     * @return a {@link ConfigurationBuilder} for the application context
     * @throws ConfigurationException when builder cannot be created
     */
    ConfigurationBuilder createConfigurationBuilder(MuleContext domainContext, List<ConfigResource> configs) throws ConfigurationException;
}
