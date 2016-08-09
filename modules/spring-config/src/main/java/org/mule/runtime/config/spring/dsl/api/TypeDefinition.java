/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.api;

import org.mule.runtime.config.spring.dsl.processor.TypeDefinitionVisitor;

/**
 * Defines the actual {@code Class} for the domain object to be created.
 */
public class TypeDefinition {

  private Class<?> type;
  private String attributeName;
  private MapEntryType mapType;

  private TypeDefinition() {}

  /**
   * Defines the object type that will be created for the component.
   *
   * Only instantiable types are allowed with the exception of {@code Collection}, {@code List}, {@code Set} interfaces. In those
   * cases the runtime will use a default implementation.
   *
   *
   * @param type {@code Class} of the domain model to be created.
   * @return {@code TypeDefinition} created from that type.
   */
  public static TypeDefinition fromType(Class<?> type) {
    TypeDefinition typeDefinition = new TypeDefinition();
    typeDefinition.type = type;
    return typeDefinition;
  }

  /**
   * @param configAttributeName name of the configuration attribute that defines the domain object type.
   * @return {@code TypeDefinition} created from that type.
   */
  public static TypeDefinition fromConfigurationAttribute(String configAttributeName) {
    TypeDefinition typeDefinition = new TypeDefinition();
    typeDefinition.attributeName = configAttributeName;
    return typeDefinition;
  }

  public void visit(TypeDefinitionVisitor typeDefinitionVisitor) {
    if (type != null) {
      typeDefinitionVisitor.onType(type);
    } else if (mapType != null) {
      typeDefinitionVisitor.onMapType(mapType);
    } else {
      typeDefinitionVisitor.onConfigurationAttribute(attributeName);
    }
  }

  public static TypeDefinition fromMapEntryType(Class<?> keyType, Class<?> valueType) {
    TypeDefinition typeDefinition = new TypeDefinition();
    typeDefinition.mapType = new MapEntryType(keyType, valueType);
    return typeDefinition;
  }

  /**
   * Instances of this class represent the type of a map entry.
   *
   * @since 4.0
   *
   * @param <KeyType> the key type
   * @param <ValueType> the value type.=
   */
  public static class MapEntryType<KeyType, ValueType> {

    private Class<KeyType> keyType;
    private Class<ValueType> valueType;

    public MapEntryType(Class<KeyType> keyType, Class<ValueType> valueType) {
      this.keyType = keyType;
      this.valueType = valueType;
    }

    /**
     * @return the key type.
     */
    public Class<?> getKeyType() {
      return keyType;
    }

    /**
     * @return the value type.
     */
    public Class<?> getValueType() {
      return valueType;
    }
  }

}
