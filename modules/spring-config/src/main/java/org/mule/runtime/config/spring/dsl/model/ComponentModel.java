/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static com.google.common.collect.ImmutableMap.copyOf;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.NAME_ATTRIBUTE;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.api.processor.MessageRouter;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * An {@code ComponentModel} represents the user configuration of a component (flow, config, message processor, etc) defined in an
 * artifact configuration file.
 * <p/>
 * Every {@code ComponentModel} represents the configuration of a core configuration or an extension configuration. Which
 * configuration element this object represents is identified by a {@link ComponentIdentifier} that can be retrieved using
 * {@code #getName}.
 * <p/>
 * It may have simple configuration parameters which are retrieve by using {@code #getParameters} or complex parameters which are
 * retrieved using {@code #getInnerComponents}.
 * <p/>
 * There's a set of configuration attributes or custom attributes that may not be mapped directly to the object that runs on
 * runtime but may be hold by a {@code ComponentModel}. Those attributes are retrieved by using {@code #getCustomAttributes}.
 *
 * @since 4.0
 */
public class ComponentModel {

  private boolean root = false;
  private ComponentIdentifier identifier;
  private Map<String, Object> customAttributes = new HashMap<>();
  private Map<String, String> parameters = new HashMap<>();
  private Set<String> schemaValueParameter = new HashSet<>();
  // TODO MULE-9638 This must go away from component model once it's immutable.
  private ComponentModel parent;
  private List<ComponentModel> innerComponents = new ArrayList<>();
  private String textContent;
  private DefaultComponentLocation componentLocation;

  // TODO MULE-9688 Remove this attributes since should not be part of this class. This class should be immutable.
  private BeanReference beanReference;
  private BeanDefinition beanDefinition;
  private Class<?> type;
  private Integer lineNumber;
  private String configFileName;
  private boolean enabled = true;

  /**
   * @return the line number in which the component was defined in the configuration file. It may be empty if the component was
   *         created pragmatically.
   */
  public Optional<Integer> getLineNumber() {
    return ofNullable(lineNumber);
  }

  /**
   * @return the config file name in which the component was defined. It may be empty if the component was created pragmatically.
   */
  public Optional<String> getConfigFileName() {
    return ofNullable(configFileName);
  }

  /**
   * @return the configuration identifier.
   */
  public ComponentIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * @return a {@code java.util.Map} with the simple parameters of the configuration.
   */
  public Map<String, String> getParameters() {
    return unmodifiableMap(parameters);
  }

  /**
   * @return a {@code java.util.List} of all the child {@code ComponentModel}s
   */
  public List<ComponentModel> getInnerComponents() {
    return innerComponents;
  }

  /**
   * @return a {@code java.util.Map} with all the custom attributes.
   */
  public Map<String, Object> getCustomAttributes() {
    return copyOf(customAttributes);
  }

  /**
   * @return true if the {@code ComponentModel} is a top level configuration element, false otherwise.
   */
  public boolean isRoot() {
    return root;
  }

  /**
   * @param beanDefinition the {@code BeanDefinition} created based on the {@code ComponentModel} values.
   */
  public void setBeanDefinition(BeanDefinition beanDefinition) {
    this.beanDefinition = beanDefinition;
  }

  /**
   * Marked as true if it's a top level configuration.
   */
  public void setRoot(boolean root) {
    this.root = root;
  }

  /**
   * @param parameterName name of the configuration parameter.
   * @param value value contained by the configuration parameter.
   */
  public void setParameter(String parameterName, String value) {
    this.parameters.put(parameterName, value);
  }

  /**
   * @return the {@code BeanDefinition} created based on the {@code ComponentModel} values.
   */
  public BeanDefinition getBeanDefinition() {
    return beanDefinition;
  }

  /**
   * @return the type of the object to be created when processing this {@code ComponentModel}.
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * @param type the type of the object to be created when processing this {@code ComponentModel}.
   */
  public void setType(Class<?> type) {
    this.type = type;
  }

  /**
   * @return the value of the name attribute.
   */
  public String getNameAttribute() {
    return parameters.get(NAME_ATTRIBUTE);
  }

  /**
   * @return true if this {@code ComponentModel} represents a {@code org.mule.runtime.core.api.processor.MessageProcessor} scope.
   */
  public boolean isScope() {
    // TODO MULE-9691 : Define a clear mechanism to realize if the object to be build is an scope. For now this works.
    return MessageRouter.class.isAssignableFrom(type);
  }

  public void setParent(ComponentModel parent) {
    this.parent = parent;
  }

  /**
   * @return the parent component model in the configuration.
   */
  public ComponentModel getParent() {
    return parent;
  }

  /**
   * @return content of the configuration element.
   */
  public String getTextContent() {
    return textContent;
  }

  /**
   * @param componentLocation the location of the component in the configuration.
   */
  public void setComponentLocation(DefaultComponentLocation componentLocation) {
    this.componentLocation = componentLocation;
  }

  /**
   * @return the location of the component in the configuration.
   */
  public DefaultComponentLocation getComponentLocation() {
    return componentLocation;
  }

  /**
   * @param beanReference the {@code BeanReference} that represents this object.
   */
  public void setBeanReference(BeanReference beanReference) {
    this.beanReference = beanReference;
  }

  /**
   * @return the {@code BeanReference} that represents this object.
   */
  public BeanReference getBeanReference() {
    return beanReference;
  }

  /**
   * @param parameterName configuration parameter name
   * @return true if the value provided for the configuration parameter was get from the DSL schema, false if it was explicitly
   *         defined in the config
   */
  public boolean isParameterValueProvidedBySchema(String parameterName) {
    return this.schemaValueParameter.contains(parameterName);
  }

  // TODO MULE-11355: Make the ComponentModel haven an ComponentConfiguration internally
  public ComponentConfiguration getConfiguration() {
    ComponentConfiguration.Builder builder = ComponentConfiguration.builder()
        .withIdentifier(this.getIdentifier())
        .withValue(textContent);

    parameters.entrySet().forEach(e -> builder.withParameter(e.getKey(), e.getValue()));
    innerComponents.forEach(i -> builder.withNestedComponent(i.getConfiguration()));
    customAttributes.forEach(builder::addCustomAttribute);

    return builder.build();
  }

  /**
   * Sets the component as enabled, meaning that it should be created and the beanDefinition associated with created too.
   *
   * @param enabled if this component is enabled and has to be created.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    this.getInnerComponents().stream().forEach(innerComponent -> innerComponent.setEnabled(enabled));
  }

  /**
   * @return {@code true} if this component is enabled and has to be created.
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Builder to create instances of {@code ComponentModel}.
   */
  public static class Builder {

    private ComponentModel model = new ComponentModel();

    /**
     * @param identifier identifier for the configuration element this object represents.
     * @return the builder.
     */
    public Builder setIdentifier(ComponentIdentifier identifier) {
      this.model.identifier = identifier;
      return this;
    }

    /**
     * @param parameterName name of the configuration parameter.
     * @param value value contained by the configuration parameter.
     * @param valueFromSchema
     * @return the builder.
     */
    public Builder addParameter(String parameterName, String value, boolean valueFromSchema) {
      this.model.parameters.put(parameterName, value);
      if (valueFromSchema) {
        this.model.schemaValueParameter.add(parameterName);
      }
      return this;
    }

    /**
     * Adds a new complex child object to this {@code ComponentModel}.
     *
     * @param componentModel child {@code ComponentModel} declared in the configuration.
     * @return the builder.
     */
    public Builder addChildComponentModel(ComponentModel componentModel) {
      this.model.innerComponents.add(componentModel);
      componentModel.setParent(model);
      return this;
    }

    /**
     * Sets the inner content of the configuration element.
     *
     * @param textContent inner text content from the configuration.
     * @return the builder.
     */
    public Builder setTextContent(String textContent) {
      this.model.textContent = textContent;
      return this;
    }

    /**
     * When invoked the created {@code ComponentModel} will be marked us a top level configuration.
     *
     * @return the builder.
     */
    public Builder markAsRootComponent() {
      this.model.root = true;
      return this;
    }

    /**
     * Adds a custom attribute to the {@code ComponentModel}. This custom attribute is meant to hold metadata of the configuration
     * and not to be used to instantiate the runtime object that corresponds to this configuration.
     *
     * @param name custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder addCustomAttribute(String name, Object value) {
      this.model.customAttributes.put(name, value);
      return this;
    }

    /**
     * @param configFileName the config file name in which this component was defined.
     * @return the builder.
     */
    public Builder setConfigFileName(String configFileName) {
      this.model.configFileName = configFileName;
      return this;
    }

    /**
     * @param lineNumber the line number within the config file in which this component was defined.
     * @return the builder.
     */
    public Builder setLineNumber(int lineNumber) {
      this.model.lineNumber = lineNumber;
      return this;
    }

    /**
     * @return a {@code ComponentModel} created based on the supplied parameters.
     */
    public ComponentModel build() {
      Preconditions.checkState(model.identifier != null, "An identifier must be provided");
      return model;
    }

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ComponentModel that = (ComponentModel) o;

    if (root != that.root) {
      return false;
    }
    if (!identifier.equals(that.identifier)) {
      return false;
    }
    if (!parameters.equals(that.parameters)) {
      return false;
    }
    return innerComponents.equals(that.innerComponents);

  }

  @Override
  public int hashCode() {
    int result = (root ? 1 : 0);
    result = 31 * result + identifier.hashCode();
    result = 31 * result + parameters.hashCode();
    result = 31 * result + innerComponents.hashCode();
    return result;
  }

}
