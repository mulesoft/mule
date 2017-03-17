/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component.config;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import org.mule.runtime.api.component.ComponentIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Defines a mule component configuration content.
 * <p>
 * A {@code ComponentConfiguration} allows to define a mule component configuration programmatically by defining the component
 * namespace, name and the set of simple attributes or complex nested attributes required by the component.
 *
 * @since 4.0
 */
// TODO MULE-11496 Delete this configuration once everything has  an ExtensionModel and can be represented with
// ComponentDeclaration
public class ComponentConfiguration {

  private ComponentIdentifier identifier;
  private Map<String, Object> customAttributes = new HashMap<>();
  private Map<String, String> parameters = new HashMap<>();
  private List<ComponentConfiguration> nestedComponents = new ArrayList<>();
  private String value;

  /**
   * @return the configuration identifier.
   */
  public ComponentIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * @return a map with the configuration parameters of the component where the key is the parameter name and the value is the
   *         parameter value.
   */
  public Map<String, String> getParameters() {
    return unmodifiableMap(parameters);
  }

  /**
   * @return content of the configuration element.
   */
  public Optional<String> getValue() {
    return Optional.ofNullable(value);
  }


  /**
   * @param name the name of the custom attribute
   * @return the custom attribute for the given name, or {@link Optional#empty()} if none was found.
   */
  public Optional<Object> getCustomAttribute(String name) {
    return Optional.ofNullable(customAttributes.get(name));
  }

  /**
   * @return a collection of the complex child configuration components.
   */
  public List<ComponentConfiguration> getNestedComponents() {
    return unmodifiableList(nestedComponents);
  }

  private ComponentConfiguration() {}

  /**
   * Builder for creating {@code ComponentConfiguration} instances.
   */
  public static class Builder {

    private ComponentConfiguration componentConfiguration = new ComponentConfiguration();

    private Builder() {}

    /**
     * @param identifier identifier for the configuration element this object represents.
     * @return the builder.
     */
    public Builder withIdentifier(ComponentIdentifier identifier) {
      componentConfiguration.identifier = identifier;
      return this;
    }

    /**
     * Adds a configuration parameter to the component
     *
     * @param name configuration attribute name
     * @param value configuration attribute value
     * @return the builder
     */
    public Builder withParameter(String name, String value) {
      componentConfiguration.parameters.put(name, value);
      return this;
    }

    /**
     * Sets the inner content of the configuration element.
     *
     * @param textContent inner text content from the configuration
     * @return the builder
     */
    public Builder withValue(String textContent) {
      componentConfiguration.value = textContent;
      return this;
    }

    /**
     * Adds a custom attribute to the {@link ComponentConfiguration}.
     * This custom attribute is meant to hold metadata of the configuration.
     *
     * @param name custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder addCustomAttribute(String name, Object value) {
      componentConfiguration.customAttributes.put(name, value);
      return this;
    }

    /**
     * Adds a complex configuration parameter to the component.
     * <p>
     * For instance, to define a file:matcher for a file:read component: *
     * 
     * <pre>
     * {@code
     * <file:read>
     *   <file:matcher regex="XYZ"/>
     * </file:read>
     * }
     * </pre>
     *
     * @param nestedComponent the {@link ComponentConfiguration} that represents the nested configuration
     * @return {@code this} {@link Builder}
     */
    public Builder withNestedComponent(ComponentConfiguration nestedComponent) {
      componentConfiguration.nestedComponents.add(nestedComponent);
      return this;
    }

    /**
     * @return a {@code ComponentConfiguration} with the provided configuration
     */
    public ComponentConfiguration build() {
      return componentConfiguration;
    }
  }

  /**
   * @return a new {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ComponentConfiguration that = (ComponentConfiguration) o;

    if (!identifier.equals(that.identifier)) {
      return false;
    }
    if (!parameters.equals(that.parameters)) {
      return false;
    }
    return nestedComponents.equals(that.nestedComponents);

  }

  @Override
  public int hashCode() {
    int result = 0;
    result = 31 * result + identifier.hashCode();
    result = 31 * result + parameters.hashCode();
    result = 31 * result + nestedComponents.hashCode();
    return result;
  }

}
