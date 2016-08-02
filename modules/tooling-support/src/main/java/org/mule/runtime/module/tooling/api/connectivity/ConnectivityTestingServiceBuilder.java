/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.connectivity;

import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;

/**
 * Builder for creating a {@code ConnectivityTestingService} from a
 * set of extensions and an {@code ArtifactConfiguration} that describes
 * a set of mule components.
 *
 * @since 4.0
 */
public interface ConnectivityTestingServiceBuilder
{

    /**
     * Adds an extension that must be used to do connectivity testing
     *
     * @param groupId group id of the extension
     * @param artifactId artifact id of the extension
     * @param artifactVersion verion of the extension
     * @return the builder
     */
    ConnectivityTestingServiceBuilder addExtension(String groupId, String artifactId, String artifactVersion);

    /**
     * Configures the mule components required to do connectivity testing
     *
     * @param artifactConfiguration set of mule components required to do connectivity testing
     * @return the builder
     */
    ConnectivityTestingServiceBuilder setArtifactConfiguration(ArtifactConfiguration artifactConfiguration);

    /**
     * Creates a {@code ConnectivityTestingService} with the provided configuration
     *
     * @return the connectivity testing service
     */
    ConnectivityTestingService build();

}
