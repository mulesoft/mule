/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api.config;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a mule component configuration content.
 * <p>
 * A {@code ComponentConfiguration} allows to define a mule component configuration programmatically
 * by defining the component namespace, identifier and the set of simple attributes or complex nested attributes
 * required by the component.
 *
 * @since 4.0
 */
public class ComponentConfiguration
{

    private String namespace;
    private String identifier;
    private Map<String, String> parameters = new HashMap<>();
    private List<ComponentConfiguration> nestedComponentConfiguration = new ArrayList<>();

    /**
     * @return the namespace where the component is located. i.e.: In file:read the namespace is file.
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * @return the identifier of the component. i.e.: In file:read the identifier is read.
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @return a map with the configuration parameters of the component where the key is the parameter name and the value is the parameter value.
     */
    public Map<String, String> getParameters()
    {
        return unmodifiableMap(parameters);
    }

    /**
     * @return a collection of the complex child configuration components.
     */
    public List<ComponentConfiguration> getNestedComponentConfiguration()
    {
        return unmodifiableList(nestedComponentConfiguration);
    }

    private ComponentConfiguration()
    {
    }

    /**
     * Builder for creating  {@code ComponentConfiguration} instances.
     */
    public static class Builder
    {

        private ComponentConfiguration componentConfiguration = new ComponentConfiguration();

        /**
         * @param namespace the namespace where the component is located. i.e.: In file:read the namespace is file
         * @return the builder
         */
        public Builder setNamespace(String namespace)
        {
            componentConfiguration.namespace = namespace;
            return this;
        }

        /**
         * @param identifier the identifier of the component. i.e.: In file:read the identifier is read.
         * @return the builder
         */
        public Builder setIdentifier(String identifier)
        {
            componentConfiguration.identifier = identifier;
            return this;
        }

        /**
         * Adds a configuration parameter to the component
         *
         * @param name  configuration attribute name
         * @param value configuration attribute value
         * @return the builder
         */
        public Builder addParameter(String name, String value)
        {
            componentConfiguration.parameters.put(name, value);
            return this;
        }

        /**
         * Adds a complex configuration parameter to the component.
         * <p>
         * For instance, to define a file:matcher for a file:read component:
         * * <pre>
         * {@code
         * <file:read>
         *   <file:matcher regex="XYZ"/>
         * </file:read>
         * }
         * </pre>
         *
         * @param componentConfiguration
         * @return
         */
        public Builder addNestedConfiguration(ComponentConfiguration componentConfiguration)
        {
            componentConfiguration.nestedComponentConfiguration.add(componentConfiguration);
            return this;
        }

        /**
         * @return a {@code ComponentConfiguration} with the provided configuration
         */
        public ComponentConfiguration build()
        {
            return componentConfiguration;
        }
    }

}
