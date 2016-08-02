/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api.config;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@code ArtifactConfiguration} is a programmatic descriptor of a mule configuration.
 *
 * @since 4.0
 */
public class ArtifactConfiguration
{

    private List<ComponentConfiguration> componentConfiguration = new ArrayList<>();

    /**
     * Creates an {@code ArtifactConfiguration} from a collection of root mule configuration components.
     *
     * Each {@link ComponentConfiguration} may have other nested configuration components.
     *
     * @param componentConfigurations collection of root configuration elements of a mule configuration. Non null.
     */
    public ArtifactConfiguration(List<ComponentConfiguration> componentConfigurations)
    {
        checkArgument(componentConfigurations != null, "component configurations cannot be null");
        this.componentConfiguration = componentConfigurations;
    }

    /**
     * @return collection of root mule configuration elements.
     */
    public List<ComponentConfiguration> getComponentConfiguration()
    {
        return componentConfiguration;
    }
}
