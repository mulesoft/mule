/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.copyOf;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;

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
 * @param <T> the actual type of the runtime object to be created.
 * 
 * @since 4.0
 */
public class ComponentBuildingDefinition<T> {

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
  private Class<? extends ObjectFactory<T>> objectFactoryType;
  private boolean prototype;
  private boolean named = false;
  private ComponentIdentifier componentIdentifier;
  private Optional<TypeConverter> typeConverter = empty();
  private Optional<TypeConverter> keyTypeConverter = empty();
  private boolean alwaysEnabled = false;

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
  public Class<? extends ObjectFactory<T>> getObjectFactoryType() {
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
   * @return if the component should be enabled even if excluded when building a partial application model
   */
  public boolean isAlwaysEnabled() {
    return alwaysEnabled;
  }

  /**
   * @return whether the defined component has a name attribute
   */
  public boolean isNamed() {
    return named;
  }

  /**
   * @return the complete list of {@link AttributeDefinition}s
   */
  public List<AttributeDefinition> getAttributesDefinitions() {
    return concat(setterParameterDefinitions.stream().map(SetterAttributeDefinition::getAttributeDefinition),
                  constructorAttributeDefinition.stream()).collect(toList());
  }

  /**
   * Builder for {@code ComponentBuildingDefinition}
   * <p/>
   */
  public static class Builder<T> {

    private String namespace;
    private String identifier;
    private ComponentBuildingDefinition<T> definition = new ComponentBuildingDefinition<>();

    /**
     * Adds a new constructor parameter to be used during the object instantiation.
     *
     * @param attributeDefinition the constructor argument definition.
     * @return a copy of {@code this} builder
     */
    public Builder<T> withConstructorParameterDefinition(AttributeDefinition attributeDefinition) {
      Builder<T> next = copy();
      next.definition.constructorAttributeDefinition.add(attributeDefinition);
      return next;
    }

    /**
     * Adds a new parameter to be added to the object by using a setter method.
     *
     * @param fieldName the name of the field in which the value must be injected
     * @param attributeDefinition the setter parameter definition
     * @return a copy of {@code this} builder
     */
    public Builder<T> withSetterParameterDefinition(String fieldName, AttributeDefinition attributeDefinition) {
      Builder<T> next = copy();
      next.definition.setterParameterDefinitions.add(new SetterAttributeDefinition(fieldName, attributeDefinition));
      return next;
    }

    /**
     * Sets the identifier of the configuration element that this building definition is for. For instance, a config element
     * <http:listener> has as identifier listener
     *
     * @param identifier configuration element identifier
     * @return a copy of {@code this} builder
     */
    public Builder<T> withIdentifier(String identifier) {
      Builder<T> next = copy();
      next.identifier = identifier;
      return next;
    }

    /**
     * Sets the namespace of the configuration element that this building definition is for. For instance, a config element
     * <http:listener> has as namespace http
     *
     * @param namespace configuration element namespace
     * @return a copy of {@code this} builder
     */
    public Builder<T> withNamespace(String namespace) {
      Builder<T> next = copy();
      next.namespace = namespace;
      return next;
    }

    /**
     * Sets the {@link TypeDefinition} to discover the object type. It may be created from {@link TypeDefinition#fromType(Class)}
     * which means the type is predefined. Or it may be created from {@link TypeDefinition#fromConfigurationAttribute(String)}
     * which means that the object type is declared within the configuration using a config attribute.
     *
     * @param typeDefinition the type definition to discover the objecvt type
     * @return a copy of {@code this} builder
     */
    public Builder<T> withTypeDefinition(TypeDefinition<T> typeDefinition) {
      Builder<T> next = copy();
      next.definition.typeDefinition = typeDefinition;
      return next;
    }

    /**
     * This method allows to convert a simple type to another type using a converter.
     *
     * {@code TypeConverter} are only allowed when the produce type by the component is any of java primitive types, its wrapper
     * or string.
     *
     * @param typeConverter converter from the configuration value to a custom type.
     * @return a copy of {@code this} builder
     */
    public Builder<T> withTypeConverter(TypeConverter typeConverter) {
      Builder<T> next = copy();
      next.definition.typeConverter = of(typeConverter);
      return next;
    }

    /**
     * This method allows to convert a map entry key to another type using a converter.
     *
     * {@code TypeConverter} are only allowed when the produce type by the component is any of java primitive types, its wrapper
     * or string.
     *
     * @param typeConverter converter from the configuration value to a custom type.
     * @return a copy of {@code this} builder
     */
    public Builder<T> withKeyTypeConverter(TypeConverter typeConverter) {
      Builder<T> next = copy();
      next.definition.keyTypeConverter = of(typeConverter);
      return next;
    }

    /**
     * This method configures the component as enabled even if excluded from a partial application model (for instance
     * when building one for data sense).
     *
     * @param value boolean indicating if the component should be always enabled
     * @return a copy of ${@code this} builder
     */
    public Builder<T> alwaysEnabled(boolean value) {
      Builder<T> next = copy();
      next.definition.alwaysEnabled = value;
      return next;
    }

    /**
     * Used to declare that the object to be created is an scope.
     *
     * @return a copy of {@code this} builder
     */
    public Builder<T> asScope() {
      Builder<T> next = copy();
      next.definition.scope = true;
      return next;
    }

    /**
     * Used to declare that the object to be created has a name attribute
     * 
     * @return a copy of {@code this} builder
     */
    public Builder<T> asNamed() {
      Builder<T> next = copy();
      next.definition.named = true;
      return next;
    }

    /**
     * Defines a factory class to be used for creating the object. This method can be used when the object to be build required
     * complex logic.
     *
     * @param objectFactoryType {@code Class} for the factory to use to create the object
     * @return a copy of {@code this} builder
     */
    public Builder<T> withObjectFactoryType(Class<? extends ObjectFactory<T>> objectFactoryType) {
      if (Initialisable.class.isAssignableFrom(objectFactoryType) ||
          Startable.class.isAssignableFrom(objectFactoryType) ||
          Stoppable.class.isAssignableFrom(objectFactoryType) ||
          Disposable.class.isAssignableFrom(objectFactoryType)) {
        throw new MuleRuntimeException(I18nMessageFactory.createStaticMessage(String
            .format("Class %s is an ObjectFactory so it cannot implement lifecycle methods",
                    objectFactoryType.getCanonicalName())));
      }
      Builder<T> next = copy();
      next.definition.objectFactoryType = objectFactoryType;
      return next;
    }

    /**
     * Mark configuration parameters to be ignored when building the component. This is mostly useful when
     * {@link AttributeDefinition.Builder#fromUndefinedSimpleAttributes()} there are certain configuration parameters that we
     * don't want to included them in the process of building the object.
     *
     * @param parameterName the configuration parameter name.
     * @return a copy of {@code this} builder.
     */
    public Builder<T> withIgnoredConfigurationParameter(String parameterName) {
      Builder<T> next = copy();
      next.definition.ignoredConfigurationParameters.add(parameterName);
      return next;
    }

    /**
     * Makes a deep copy of the builder so it's current configuration can be reused.
     *
     * This is called automatically on each method to make sure users don't accidentally modify the original when intending to
     * refactor common cases.
     *
     * @return a {@code Builder} copy.
     */
    private Builder<T> copy() {
      Builder<T> builder = new Builder<>();
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
        builder.definition.named = true;
      }

      return builder;
    }

    /**
     * Builds a {@link ComponentBuildingDefinition} with the parameters set in the builder.
     * <p/>
     * At least the identifier, namespace and type definition must be configured or this method will fail.
     *
     * @return a fully configured {@link ComponentBuildingDefinition}
     */
    public ComponentBuildingDefinition<T> build() {
      checkState(definition.typeDefinition != null, "You must specify the type");
      checkState(identifier != null, "You must specify the identifier");
      checkState(namespace != null, "You must specify the namespace");
      Optional<Class> componentType = getType();
      checkState(!definition.typeConverter.isPresent()
          || (definition.typeConverter.isPresent() && componentType.isPresent()),
                 TYPE_CONVERTER_AND_UNKNOWN_TYPE_MESSAGE);
      checkState(!definition.typeConverter.isPresent()
          || (definition.typeConverter.isPresent()
              && (DslSimpleType.isSimpleType(componentType.get()) || isMapType(componentType.get()))),
                 format(TYPE_CONVERTER_AND_NO_SIMPLE_TYPE_MESSAGE_TEMPLATE,
                        componentType.orElse(Object.class).getName()));
      checkState(!definition.keyTypeConverter.isPresent()
          || (definition.keyTypeConverter.isPresent() && componentType.isPresent() && isMapType(componentType.get())),
                 KEY_TYPE_CONVERTER_AND_NO_MAP_TYPE);
      definition.componentIdentifier =
          builder().name(identifier).namespace(namespace).build();
      return definition;
    }

    private boolean isMapType(Class componentType) {
      return Map.class.isAssignableFrom(componentType);
    }

    // TODO MULE-9681: remove for some other semantic. The API should not define something as "prototype" it should declare if
    // it's a reusable component or an instance.
    // Ideally this can be inferred by the language itself. e.g.: Global message processors are always reusable components and do
    // not define entities by them self.
    public Builder<T> asPrototype() {
      Builder<T> next = copy();
      next.definition.prototype = true;
      return next;
    }

    private Optional<Class> getType() {
      final AtomicReference<Class> typeReference = new AtomicReference<>();
      definition.typeDefinition.visit(new TypeDefinitionVisitor() {

        @Override
        public void onType(Class<?> type) {
          typeReference.set(type);
        }

        @Override
        public void onConfigurationAttribute(String attributeName, Class<?> enforcedClass) {}

        @Override
        public void onMapType(TypeDefinition.MapEntryType mapEntryType) {
          typeReference.set(Map.class);
        }
      });
      return ofNullable(typeReference.get());
    }
  }
}
