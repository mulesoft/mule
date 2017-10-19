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
import org.mule.runtime.api.component.location.ComponentLocation;

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
// TODO MULE-11496 Delete this configuration once everything has an ExtensionModel and can be represented with
// ComponentDeclaration
public abstract class ComponentConfiguration {

  protected ComponentIdentifier identifier;
  protected ComponentLocation componentLocation;
  protected Map<String, Object> properties = new HashMap<>();
  protected Map<String, String> parameters = new HashMap<>();
  protected List<ComponentConfiguration> nestedComponents = new ArrayList<>();
  protected String value;

  /**
   * @return the configuration identifier.
   */
  public ComponentIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * @return the location of the component in the configuration
   */
  public ComponentLocation getComponentLocation() {
    return componentLocation;
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
   * @param name the name of the property
   * @return the property for the given name, or {@link Optional#empty()} if none was found.
   */
  public Optional<Object> getProperty(String name) {
    return Optional.ofNullable(properties.get(name));
  }

  /**
   * @return a collection of the complex child configuration components.
   */
  public List<ComponentConfiguration> getNestedComponents() {
    return unmodifiableList(nestedComponents);
  }

  protected ComponentConfiguration() {}

}
