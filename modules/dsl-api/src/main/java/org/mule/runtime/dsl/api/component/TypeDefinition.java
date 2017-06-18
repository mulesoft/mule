/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.dsl.api.component;

/**
 * Defines the actual {@code Class} for the domain object to be created.
 * 
 * @param <T> the actual type of the runtime object to be created.
 */
public class TypeDefinition<T> {

  private Class<T> type;
  private String attributeName;
  private MapEntryType mapType;
  private Class<?> inforcedClass;

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
  public static <T> TypeDefinition<T> fromType(Class<T> type) {
    TypeDefinition<T> typeDefinition = new TypeDefinition<>();
    typeDefinition.type = type;
    return typeDefinition;
  }

  /**
   * @param configAttributeName name of the configuration attribute that defines the domain object type.
   * @return {@code TypeDefinition} created from that type.
   */
  public static <T> TypeDefinition<T> fromConfigurationAttribute(String configAttributeName) {
    TypeDefinition<T> typeDefinition = new TypeDefinition<>();
    typeDefinition.attributeName = configAttributeName;
    return typeDefinition;
  }

  /**
   * @param inforcedClass class to be checked as the same or a super class of the type if defined as a config attribute
   * @return {@code TypeDefinition} whith className set.
   */
  public TypeDefinition<T> checkingThatIsClassOrInheritsFrom(Class<?> inforcedClass) {
    this.inforcedClass = inforcedClass;
    return this;
  }


  public void visit(TypeDefinitionVisitor typeDefinitionVisitor) {
    if (type != null) {
      typeDefinitionVisitor.onType(type);
    } else if (mapType != null) {
      typeDefinitionVisitor.onMapType(mapType);
    } else if (inforcedClass != null) {
      typeDefinitionVisitor.onConfigurationAttribute(attributeName, inforcedClass);
    } else {
      typeDefinitionVisitor.onConfigurationAttribute(attributeName, Object.class);
    }
  }

  public static <T, K, V> TypeDefinition<T> fromMapEntryType(Class<K> keyType, Class<V> valueType) {
    TypeDefinition<T> typeDefinition = new TypeDefinition<>();
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
