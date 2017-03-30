/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.dsl.api.component;

import java.util.Optional;

/**
 * An {code AttributeDefinitionVisitor} allows to access an {@link AttributeDefinition} configuration.
 * <p>
 * Depending on the {@link AttributeDefinition} configuration a method and only one method
 * of this contract will be invoked.
 *
 * @since 4.0
 */
public interface AttributeDefinitionVisitor {

  /**
   * Called when the attribute needs to be configured from an object provided by the Mule API.
   *
   * @param objectType the expected object type.
   */
  void onReferenceObject(Class<?> objectType);

  /**
   * Called when the attribute must be configured from another object defined in the configuration.
   *
   * @param reference the identifier of an object declared in the configuration.
   */
  void onReferenceSimpleParameter(String reference);

  /**
   * Called when the attribute must be configured from another fixed object, from which we have a reference
   *
   * @param reference the identifier of an object.
   */
  void onReferenceFixedParameter(String reference);

  /**
   * To be called when the value to be set when building the object is fixed and provided by the definition of the
   * {@link ComponentBuildingDefinition}.
   *
   * @param value the fixed value
   */
  void onFixedValue(Object value);

  /**
   * Called when the attribute is configured from a simple configuration attribute.
   *
   * @param parameterName configuration parameter name.
   * @param defaultValue default value for the configuration parameter if it has not value.
   * @param typeConverter a value converter to convert from the value provided by the config to the value required of the
   *        attribute.
   */
  void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter);

  /**
   * Called when the attribute holds all the simple configuration attributes not mapped to any other attribute.
   */
  void onUndefinedSimpleParameters();

  /**
   * Called when the attribute holds all the complex configuration attributes not mapped to any other attribute.
   */
  void onUndefinedComplexParameters();

  /**
   * Called when the attribute is configured from a list of object with a certain type.
   *
   * @param type type of the list values to be set in the attribute.
   * @param wrapperIdentifierOptional the identifier of the wrapper element that holds the list of components
   */
  void onComplexChildCollection(Class<?> type, Optional<String> wrapperIdentifierOptional);

  /**
   * Called when the attribute is configured from a map of objects with a certain type.
   *
   * @param keyType type of the map key to be set in the attribute.
   * @param valueType type of the map value to be set in the attribute.
   * @param wrapperIdentifier the identifier of the wrapper element that holds the list of components
   */
  void onComplexChildMap(Class<?> keyType, Class<?> valueType, String wrapperIdentifier);

  /**
   * Called when the attribute is configured from an object with a certain type.
   * 
   * @param type type of the attribute value.
   * @param wrapperIdentifier the identifier of the component
   * @param childIdentifier
   */
  void onComplexChild(Class<?> type, Optional<String> wrapperIdentifier, Optional<String> childIdentifier);

  /**
   * Called when the attribute is configured from the {@code ComponentModel} inner configuration.
   */
  void onValueFromTextContent();

  /**
   * Called when a multiple configuration parameters or children components objects need to be set in single object attribute or
   * constructor parameter. The value to be set is a @{code Map} with the {@code KeyAttributeDefinitionPair#getKey()} as key and
   * the value is the resolved parameter value or component object.
   *
   * @param definitions the set of {@code AttributeDefinition} to be used to create
   */
  void onMultipleValues(KeyAttributeDefinitionPair[] definitions);

}
