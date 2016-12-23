/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;

/**
 * Utility class for parameter declaration to be able to give metadata of from which component
 * this parameter is part of.
 *
 * @since 4.0
 */
public final class ParameterDeclarationContext {

  private final String componentType;
  private final NamedDeclaration declaration;

  public ParameterDeclarationContext(String componentType, NamedDeclaration declaration) {
    this.componentType = componentType;
    this.declaration = declaration;
  }

  /**
   * @return The component name: the Configuration name, Operation name, etc.
   */
  public String getName() {
    return declaration.getName();
  }

  /**
   * @return The component type: Operation, Source, etc.
   */
  public String getComponentType() {
    return componentType;
  }
}
