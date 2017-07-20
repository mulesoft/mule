/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

/**
 * Visitor that will be invoked based on a
 * {@link ComponentBuildingDefinition#getTypeDefinition()} configuration.
 *
 * @since 4.0
 */
public interface TypeDefinitionVisitor {

  /**
   * Invoked when the {@link TypeDefinition} it's defined from a {@code Class} hardcoded value
   *
   * @param type the hardcoded type
   */
  void onType(Class<?> type);

  /**
   * Invoked when the {@link TypeDefinition} it's defined from a configuration attribute of the component
   *
   * @param attributeName the name of the configuration attribute holding the type definition. Most likely a fully qualified java
   *        class name.
   * @param enforcedClass the name of the class from which the one defined in the attribute must be an instance of.
   */
  void onConfigurationAttribute(String attributeName, Class<?> enforcedClass);

  /**
   * Invoked when the {@link TypeDefinition} it's defined to be a map entry.
   *
   * @param mapEntryType the holder for the key type and value type
   */
  void onMapType(TypeDefinition.MapEntryType mapEntryType);
}
