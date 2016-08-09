/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.config.spring.dsl.api.TypeDefinition;

/**
 * Visitor that will be invoked based on a
 * {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition#getTypeDefinition()} configuration.
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
   */
  void onConfigurationAttribute(String attributeName);

  /**
   * Invoked when the {@link TypeDefinition} it's defined to be a map entry.
   *
   * @param mapEntryType the holder for the key type and value type
   */
  void onMapType(TypeDefinition.MapEntryType mapEntryType);
}
