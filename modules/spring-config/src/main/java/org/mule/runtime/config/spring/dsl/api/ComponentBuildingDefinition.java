/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.config.spring.dsl.spring.DslSimpleType.isSimpleType;
import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.core.config.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.processor.TypeDefinitionVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Defines the mapping between a component configuration and how the object that represents that model in runtime is created.
 *
 * @since 4.0
 */
public class ComponentBuildingDefinition {

  public static final String TYPE_CONVERTER_AND_UNKNOWN_TYPE_MESSAGE =
      "Type converter cannot be used with a type definition from a configuration attribute.";
  public static final String TYPE_CONVERTER_AND_NO_SIMPLE_TYPE_MESSAGE_TEMPLATE =
      "Type converter can only be used with simple types. You can't use it with %s";
  public static final String KEY_TYPE_CONVERTER_AND_NO_MAP_TYPE = "key type converter can only be used with objects of type Map";

  private TypeDefinition typeDefinition;
  private boolean scope;
  private List<AttributeDefinition> constructorAttributeDefinition = new ArrayList<>();
  private List<SetterAttributeDefinition> setterParameterDefinitions = new ArrayList<>();
  private Set<String> ignoredConfigurationParameters = new HashSet<>();
  // TODO MULE-9638 Use generics. Generics cannot be used right now because this method colides with the ones defined in
  // FactoryBeans.
  private Class<?> objectFactoryType;
  private boolean prototype;
  private boolean named = false;
  private ComponentIdentifier componentIdentifier;
  private Optional<TypeConverter> typeConverter = empty();
  private Optional<TypeConverter> keyTypeConverter = empty();

  private ComponentBuildingDefinition() {}

  /**
   * @return a definition for the object type that must be created for this component
   */
  public TypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

  /**
   * @return true if the building definition is an scope of message processors
   */
  public boolean isScope() {
    return scope;
  }

  /**
   * @return an ordered list of the constructor parameters that must be set to create the domain object
   */
  public List<AttributeDefinition> getConstructorAttributeDefinition() {
    return constructorAttributeDefinition;
  }

  /**
   * @return a list of the attributes and its definitions that may contain configuration for the domain object to be created.
   */
  public List<SetterAttributeDefinition> getSetterParameterDefinitions() {
    return setterParameterDefinitions;
  }

  public Set<String> getIgnoredConfigurationParameters() {
    return copyOf(ignoredConfigurationParameters);
  }

  /**
   * @return the factory for the domain object. For complex object creations it's possible to define an object builder that will
   *         end up creating the domain object.
   */
  public Class<?> getObjectFactoryType() {
    return objectFactoryType;
  }

  /**
   * @return if the object is a prototype or a singleton
   */
  // TODO MULE-9681: remove for some other semantic. The API should not define something as "prototype" it should declare if it's
  // a reusable component or an instance.
  // Ideally this can be inferred by the language itself. e.g.: Global message processors are always reusable components and do
  // not define entities by them self.
  public boolean isPrototype() {
    return prototype;
  }

  /**
   * @return the unique identifier for this component
   */
  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  /**
   * @return a converter to be applied to the configuration value.
   */
  public Optional<TypeConverter> getTypeConverter() {
    return typeConverter;
  }

  /**
   * @return a converter to be applied to the configuration key when the element is a map entry.
   */
  public Optional<TypeConverter> getKeyTypeConverter() {
    return keyTypeConverter;
  }

  /**
   * @return whether the defined component has a name attribute
   */
  public boolean isNamed() {
    return named;
  }

  /**
   * Builder for {@code ComponentBuildingDefinition}
   * <p/>
   * TODO MULE-9693 Improve builder so the copy is not required to reuse the namespace value.
   */
  public static class Builder {

    private String namespace;
    private String identifier;
    private ComponentBuildingDefinition definition = new ComponentBuildingDefinition();

    /**
     * Adds a new constructor parameter to be used during the object instantiation.
     *
     * @param attributeDefinition the constructor argument definition.
     * @return {@code this} builder
     */
    public Builder withConstructorParameterDefinition(AttributeDefinition attributeDefinition) {
      definition.constructorAttributeDefinition.add(attributeDefinition);
      return this;
    }

    /**
     * Adds a new parameter to be added to the object by using a setter method.
     *
     * @param fieldName the name of the field in which the value must be injected
     * @param attributeDefinition the setter parameter definition
     * @return {@code this} builder
     */
    public Builder withSetterParameterDefinition(String fieldName, AttributeDefinition attributeDefinition) {
      definition.setterParameterDefinitions.add(new SetterAttributeDefinition(fieldName, attributeDefinition));
      return this;
    }

    /**
     * Sets the identifier of the configuration element that this building definition is for. For instance, a config element
     * <http:listener> has as identifier listener
     *
     * @param identifier configuration element identifier
     * @return {@code this} builder
     */
    public Builder withIdentifier(String identifier) {
      this.identifier = identifier;
      return this;
    }

    /**
     * Sets the namespace of the configuration element that this building definition is for. For instance, a config element
     * <http:listener> has as namespace http
     *
     * @param namespace configuration element namespace
     * @return {@code this} builder
     */
    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    /**
     * Sets the {@link TypeDefinition} to discover the object type. It may be created from {@link TypeDefinition#fromType(Class)}
     * which means the type is predefined. Or it may be created from {@link TypeDefinition#fromConfigurationAttribute(String)}
     * which means that the object type is declared within the configuration using a config attribute.
     *
     * @param typeDefinition the type definition to discover the objecvt type
     * @return {@code this} builder
     */
    public Builder withTypeDefinition(TypeDefinition typeDefinition) {
      definition.typeDefinition = typeDefinition;
      return this;
    }

    /**
     * This method allows to convert a simple type to another type using a converter.
     *
     * {@code TypeConverter} are only allowed when the produce type by the component is any of java primitive types, its wrapper
     * or string.
     *
     * @param typeConverter converter from the configuration value to a custom type.
     * @return {@code this} builder
     * @return
     */
    public Builder withTypeConverter(TypeConverter typeConverter) {
      definition.typeConverter = of(typeConverter);
      return this;
    }

    /**
     * This method allows to convert a map entry key to another type using a converter.
     *
     * {@code TypeConverter} are only allowed when the produce type by the component is any of java primitive types, its wrapper
     * or string.
     *
     * @param typeConverter converter from the configuration value to a custom type.
     * @return {@code this} builder
     * @return
     */
    public Builder withKeyTypeConverter(TypeConverter typeConverter) {
      definition.keyTypeConverter = of(typeConverter);
      return this;
    }

    /**
     * Used to declare that the object to be created is an scope.
     *
     * @return {@code this} builder
     */
    public Builder asScope() {
      definition.scope = true;
      return this;
    }

    /**
     * Used to declare that the object to be created has a name attribute
     * 
     * @return {@code this} builder
     */
    public Builder asNamed() {
      definition.named = true;
      return this;
    }

    /**
     * Defines a factory class to be used for creating the object. This method can be used when the object to be build required
     * complex logic.
     *
     * @param objectFactoryType {@code Class} for the factory to use to create the object
     * @return {@code this} builder
     */
    public Builder withObjectFactoryType(Class<?> objectFactoryType) {
      definition.objectFactoryType = objectFactoryType;
      return this;
    }

    /**
     * Mark configuration parameters to be ignored when building the component. This is mostly useful when
     * {@link org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder#fromUndefinedSimpleAttributes()} is used an there
     * are certain configuration parameters that we don't want to included them.
     *
     * @param parameterName the configuration parameter name.
     * @return {@code this} builder.
     */
    public Builder withIgnoredConfigurationParameter(String parameterName) {
      definition.ignoredConfigurationParameters.add(parameterName);
      return this;
    }

    /**
     * Makes a deep copy of the builder so it's current configuration can be reused.
     *
     * @return a {@code Builder} copy.
     */
    public Builder copy() {
      Builder builder = new Builder();
      builder.definition.typeDefinition = this.definition.typeDefinition;
      builder.definition.setterParameterDefinitions = new ArrayList<>(this.definition.setterParameterDefinitions);
      builder.definition.constructorAttributeDefinition = new ArrayList<>(this.definition.constructorAttributeDefinition);
      builder.identifier = this.identifier;
      builder.namespace = this.namespace;
      builder.definition.prototype = this.definition.prototype;
      builder.definition.scope = this.definition.scope;
      builder.definition.typeDefinition = this.definition.typeDefinition;
      builder.definition.objectFactoryType = this.definition.objectFactoryType;

      if (definition.isNamed()) {
        builder.asNamed();
      }

      return builder;
    }

    /**
     * Builds a {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition} with the parameters set in the builder.
     * <p/>
     * At least the identifier, namespace and type definition must be configured or this method will fail.
     *
     * @return a fully configured {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition}
     */
    public ComponentBuildingDefinition build() {
      checkState(definition.typeDefinition != null, "You must specify the type");
      checkState(identifier != null, "You must specify the identifier");
      checkState(namespace != null, "You must specify the namespace");
      Optional<Class> componentType = getType();
      checkState(!definition.typeConverter.isPresent() || (definition.typeConverter.isPresent() && componentType.isPresent()),
                 TYPE_CONVERTER_AND_UNKNOWN_TYPE_MESSAGE);
      checkState(!definition.typeConverter.isPresent()
          || (definition.typeConverter.isPresent() && (isSimpleType(componentType.get()) || isMapType(componentType.get()))),
                 format(TYPE_CONVERTER_AND_NO_SIMPLE_TYPE_MESSAGE_TEMPLATE, componentType.orElse(Object.class).getName()));
      checkState(!definition.keyTypeConverter.isPresent()
          || (definition.keyTypeConverter.isPresent() && componentType.isPresent() && isMapType(componentType.get())),
                 KEY_TYPE_CONVERTER_AND_NO_MAP_TYPE);
      definition.componentIdentifier = new ComponentIdentifier.Builder().withName(identifier).withNamespace(namespace).build();
      return definition;
    }

    private boolean isMapType(Class componentType) {
      return Map.class.isAssignableFrom(componentType);
    }

    // TODO MULE-9681: remove for some other semantic. The API should not define something as "prototype" it should declare if
    // it's a reusable component or an instance.
    // Ideally this can be inferred by the language itself. e.g.: Global message processors are always reusable components and do
    // not define entities by them self.
    public Builder asPrototype() {
      definition.prototype = true;
      return this;
    }

    private Optional<Class> getType() {
      final AtomicReference<Class> typeReference = new AtomicReference<>();
      definition.typeDefinition.visit(new TypeDefinitionVisitor() {

        @Override
        public void onType(Class<?> type) {
          typeReference.set(type);
        }

        @Override
        public void onConfigurationAttribute(String attributeName) {}

        @Override
        public void onMapType(TypeDefinition.MapEntryType mapEntryType) {
          typeReference.set(Map.class);
        }
      });
      return ofNullable(typeReference.get());
    }
  }
}
