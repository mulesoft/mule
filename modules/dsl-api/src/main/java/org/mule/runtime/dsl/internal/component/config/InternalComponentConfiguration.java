/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.component.config;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

public class InternalComponentConfiguration extends ComponentConfiguration {

  public InternalComponentConfiguration() {}

  /**
   * Builder for creating {@code ComponentConfiguration} instances.
   */
  public static class Builder {

    private InternalComponentConfiguration componentConfiguration = new InternalComponentConfiguration();

    /**
     * @return the location of the component in the configuration.
     */
    public InternalComponentConfiguration getComponentConfiguration() {
      return componentConfiguration;
    }

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
     * Adds a property to the {@link ComponentConfiguration}. This property is meant to hold only metadata of the configuration.
     *
     * @param name custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder withProperty(String name, Object value) {
      componentConfiguration.properties.put(name, value);
      return this;
    }

    /**
     * @param componentLocation the location of the component in the configuration.
     * @return the builder.
     */
    public Builder withComponentLocation(ComponentLocation componentLocation) {
      componentConfiguration.componentLocation = componentLocation;
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

    InternalComponentConfiguration that = (InternalComponentConfiguration) o;

    if (!getIdentifier().equals(that.getIdentifier())) {
      return false;
    }
    if (getComponentLocation() != null ? !getComponentLocation().equals(that.getComponentLocation())
        : that.getComponentLocation() != null) {
      return false;
    }
    if (!properties.equals(that.properties)) {
      return false;
    }
    if (!getParameters().equals(that.getParameters())) {
      return false;
    }
    if (!getNestedComponents().equals(that.getNestedComponents())) {
      return false;
    }
    return getValue() != null ? getValue().equals(that.getValue()) : that.getValue() == null;
  }

  @Override
  public int hashCode() {
    int result = getIdentifier().hashCode();
    result = 31 * result + (getComponentLocation() != null ? getComponentLocation().hashCode() : 0);
    result = 31 * result + properties.hashCode();
    result = 31 * result + getParameters().hashCode();
    result = 31 * result + getNestedComponents().hashCode();
    result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
    return result;
  }

}
