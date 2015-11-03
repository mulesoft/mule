/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.DefaultMuleApplication;
import org.mule.module.launcher.artifact.Artifact;

/**
 * A domain is a deployable Artifact that contains shared resources for {@link org.mule.module.launcher.application.Application}
 * <p/>
 * A domain can just consist of a set of jar libraries to share between the domain applications or it can also contain shared
 * resources such as connectors or other mule components.
 */
public interface Domain extends Artifact
{
    /**
     * Domain configuration file name
     */
    String DOMAIN_CONFIG_FILE_LOCATION = "mule-domain-config.xml";

    /**
     * @return true if this domain has shared mule components, false if it doesn't
     */
    boolean containsSharedResources();

    /**
     * @return the MuleContext created with the domain resources. It can return null if it doesn't contains shared resources
     */
    MuleContext getMuleContext();

    /**
     * Creates a {@link ConfigurationBuilder} for a certain application which is going to be configured
     * to use the correct domain.
     *
     * @param application application for which the configuration builder should be created.
     * @return configuration builder to getDomainClassLoader the application
     * @throws Exception
     */
    ConfigurationBuilder createApplicationConfigurationBuilder(Application application) throws Exception;
}
