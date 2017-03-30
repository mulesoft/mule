/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;

import java.util.Optional;

/**
 * Defines how to build an attribute from an object.
 * <p/>
 * An attribute may be configured to be set by using a constructor or a setter.
 * <p/>
 * The {@link AttributeDefinition.Builder} allows to create an {@code AttributeDefinition}
 * from many different sources.
 *
 * @since 4.0
 */
public class AttributeDefinition {

  private String configParameterName;
  private Object defaultValue;
  private boolean hasDefaultValue;
  private boolean undefinedSimpleParametersHolder;
  private Class<?> referenceObject;
  private String referenceFixedParameter;
  private Class<?> childObjectType;
  private boolean undefinedComplexParametersHolder;
  private String referenceSimpleParameter;
  private boolean collection;
  private boolean map;
  private Class<?> mapKeyType;
  private boolean valueFromTextContent;
  private TypeConverter typeConverter;
  private KeyAttributeDefinitionPair[] definitions;
  private String wrapperIdentifier;
  private String childIdentifier;


  private AttributeDefinition() {}

  /**
   * @param visitor handler for the configuration option set for this parameter.
   */
  public void accept(AttributeDefinitionVisitor visitor) {
    if (configParameterName != null) {
      visitor.onConfigurationParameter(configParameterName, defaultValue, ofNullable(typeConverter));
    } else if (referenceObject != null) {
      visitor.onReferenceObject(referenceObject);
    } else if (undefinedSimpleParametersHolder) {
      visitor.onUndefinedSimpleParameters();
    } else if (undefinedComplexParametersHolder) {
      visitor.onUndefinedComplexParameters();
    } else if (referenceSimpleParameter != null) {
      visitor.onReferenceSimpleParameter(referenceSimpleParameter);
    } else if (referenceFixedParameter != null) {
      visitor.onReferenceFixedParameter(referenceFixedParameter);
    } else if (childObjectType != null && collection) {
      visitor.onComplexChildCollection(childObjectType, ofNullable(wrapperIdentifier));
    } else if (childObjectType != null && map) {
      visitor.onComplexChildMap(mapKeyType, childObjectType, wrapperIdentifier);
    } else if (childObjectType != null) {
      Optional<String> wrapperIdentifier = ofNullable(this.wrapperIdentifier);
      Optional<String> childIdentifier = ofNullable(this.childIdentifier);
      visitor.onComplexChild(childObjectType, wrapperIdentifier, childIdentifier);
    } else if (valueFromTextContent) {
      visitor.onValueFromTextContent();
    } else if (definitions != null) {
      visitor.onMultipleValues(definitions);
    } else if (hasDefaultValue) {
      visitor.onFixedValue(defaultValue);
    } else {
      throw new RuntimeException();
    }
  }

  public static class Builder {

    private AttributeDefinition attributeDefinition = new AttributeDefinition();

    private Builder() {}

    /**
     * @param configParameterName name of the configuration parameter from which this attribute value will be extracted.
     * @return the builder
     */
    public static Builder fromSimpleParameter(String configParameterName) {
      Builder builder = new Builder();
      builder.attributeDefinition.configParameterName = configParameterName;
      return builder;
    }

    /**
     * @param configParameterName name of the configuration parameter from which this attribute value will be extracted.
     * @param typeConverter converter from the configuration value to a custom type.
     * @return the builder
     */
    public static Builder fromSimpleParameter(String configParameterName, TypeConverter typeConverter) {
      Builder builder = new Builder();
      builder.attributeDefinition.configParameterName = configParameterName;
      builder.attributeDefinition.typeConverter = typeConverter;
      return builder;
    }

    /**
     * @param defaultValue defines the default value to be used for the attribute if no other value is provided.
     * @return the builder
     */
    public Builder withDefaultValue(Object defaultValue) {
      attributeDefinition.hasDefaultValue = true;
      attributeDefinition.defaultValue = defaultValue;
      return this;
    }

    /**
     * Defines the parent identifier used to wrap a child element. Useful when there are children with the same type and we need
     * to make a distinction to know how to do injection over multiple attributes with the same type. The identifier provided does
     * not require a component definition since it will be just for qualifying a child.
     *
     * i.e.:
     * 
     * <pre>
     *     <parent-component>
     *         <first-wrapper>
     *             <child-component>
     *         </first-wrapper>
     *         <second-wrapper>
     *             <child-component>
     *         </second-wrapper>
     *     </parent-component>
     * </pre>
     *
     * The first-wrapper and second-wrapper elements are just used to univocally identify the object attribute in which the
     * child-component object must be injected.
     *
     * @param identifier component identifier in the configuration for the parent element wrapping the child component
     * @return
     */
    public Builder withWrapperIdentifier(String identifier) {
      checkState(attributeDefinition.childObjectType != null, "Identifier can only be used with children component definitions");
      attributeDefinition.wrapperIdentifier = identifier;
      return this;
    }

    /**
     * @param value a fixed value which will be assigned to the attribute.
     * @return the builder
     */
    public static Builder fromFixedValue(Object value) {
      Builder builder = new Builder();
      builder.attributeDefinition.hasDefaultValue = true;
      builder.attributeDefinition.defaultValue = value;
      return builder;
    }

    /**
     * Use when the reference is fixed (and not configurable), not the value.
     *
     * @param reference a fixed reference object which will be assigned to the attribute.
     * @return the builder
     */
    public static Builder fromFixedReference(String reference) {
      Builder builder = new Builder();
      builder.attributeDefinition.referenceFixedParameter = reference;
      return builder;
    }

    /**
     * Calling this method declares that the attribute will be assigned with all declared simple configuration attribute and its
     * value. By simple attribute we consider those with a key and a string value as content.
     * <p/>
     * The simple attributes are store in a {@code java.util.Map} so the attribute type must also be a {@code java.util.Map}.
     *
     * @return the builder
     */
    public static Builder fromUndefinedSimpleAttributes() {
      Builder builder = new Builder();
      builder.attributeDefinition.undefinedSimpleParametersHolder = true;
      return builder;
    }

    /**
     * Used when attribute an attribute must be set with an object provided by the runtime. For instance when the object requires
     * access to the {@code org.mule.runtime.core.api.MuleContext} or a {@code org.mule.runtime.core.time.TimeSupplier}.
     *
     * @param referenceObjectType type of the object expected to be injected.
     * @return the builder
     */
    public static Builder fromReferenceObject(Class<?> referenceObjectType) {
      Builder builder = new Builder();
      builder.attributeDefinition.referenceObject = referenceObjectType;
      return builder;
    }

    /**
     * Used when an attribute must be set with a complex object created from the user configuration.
     *
     * @param childType type of the required complex object.
     * @return the builder
     */
    public static Builder fromChildConfiguration(Class<?> childType) {
      Builder builder = new Builder();
      builder.attributeDefinition.childObjectType = childType;
      return builder;
    }

    /**
     * Calling this method declares that the attribute will be assigned with all declared complex configuration object that did
     * not were map by other {@code AttributeDefinition}s. By complex attribute we consider those that are represented by complex
     * object types.
     * <p/>
     * The complex attributes are store in a {@code java.util.List} so the attribute type must also be a {@code java.util.List}.
     *
     * @return the builder
     */
    public static Builder fromUndefinedComplexAttribute() {
      Builder builder = new Builder();
      builder.attributeDefinition.undefinedComplexParametersHolder = true;
      return builder;
    }

    /**
     * @param referenceSimpleParameter configuration attribute that holds a reference to another configuration object.
     * @return the builder
     */
    public static Builder fromSimpleReferenceParameter(String referenceSimpleParameter) {
      Builder builder = new Builder();
      builder.attributeDefinition.referenceSimpleParameter = referenceSimpleParameter;
      return builder;
    }

    /**
     * Used when an attribute must be set with a collection of objects created from the user configuration.
     *
     * @param type the collection object type.
     * @return the builder
     */
    public static Builder fromChildCollectionConfiguration(Class<?> type) {
      Builder builder = new Builder();
      builder.attributeDefinition.childObjectType = type;
      builder.attributeDefinition.collection = true;
      return builder;
    }

    /**
     * Used when an attribute must be set with a map of objects created from the user configuration.
     *
     * @param keyType the map key type.
     * @param valueType the map value type.
     * @return the builder
     */
    public static Builder fromChildMapConfiguration(Class<?> keyType, Class<?> valueType) {
      Builder builder = new Builder();
      builder.attributeDefinition.childObjectType = valueType;
      builder.attributeDefinition.mapKeyType = keyType;
      builder.attributeDefinition.map = true;
      return builder;
    }

    /**
     * Used when an attribute must be created with the inner content of the configuration element.
     *
     * @return the builder
     */
    public static Builder fromTextContent() {
      Builder builder = new Builder();
      builder.attributeDefinition.valueFromTextContent = true;
      return builder;
    }

    /**
     * Used when several attributes or child components needs to be mapped to a single attribute. The attribute must be of type
     * Map where the key are the attribute name or the child element name and the value is the actual object.
     *
     * @param definitions the set of attribute definitions along with its keys
     * @return the builder
     */
    public static Builder fromMultipleDefinitions(KeyAttributeDefinitionPair... definitions) {
      Builder builder = new Builder();
      builder.attributeDefinition.definitions = definitions;
      return builder;
    }

    public Builder withIdentifier(String childIdentifier) {
      attributeDefinition.childIdentifier = childIdentifier;
      return this;
    }

    /**
     * @return the {@code AttributeDefinition} created based on the defined configuration.
     */
    public AttributeDefinition build() {
      return attributeDefinition;
    }
  }

}
