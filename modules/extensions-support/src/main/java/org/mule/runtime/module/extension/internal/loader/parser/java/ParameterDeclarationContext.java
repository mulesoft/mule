/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.module.extension.internal.loader.parser.java.StackableTypesModelPropertyResolver.newInstance;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;

import java.util.List;

/**
 * Utility class for parameter declaration to be able to give metadata of from which component this parameter is part of.
 *
 * @since 4.0
 */
public final class ParameterDeclarationContext {

  private static final StackableTypesModelPropertyResolver STACKABLE_TYPES_RESOLVER = newInstance();

  private final String componentName;
  private final String componentType;

  private final boolean keyResolverAvailable;

  public static ParameterDeclarationContext forConfig(String configName) {
    return new ParameterDeclarationContext(configName, "Configuration");
  }

  public static ParameterDeclarationContext forOperation(String operationName) {
    return new ParameterDeclarationContext(operationName, "Operation");
  }

  public static ParameterDeclarationContext forOperation(String operationName, boolean keyResolverAvailable) {
    return new ParameterDeclarationContext(operationName, "Operation", keyResolverAvailable);
  }

  public static ParameterDeclarationContext forSource(String sourceName) {
    return new ParameterDeclarationContext(sourceName, "Source");
  }

  public static ParameterDeclarationContext forSource(String sourceName, boolean keyResolverAvailable) {
    return new ParameterDeclarationContext(sourceName, "Source", keyResolverAvailable);
  }

  public static ParameterDeclarationContext forConnectionProvider(String connectionProviderName) {
    return new ParameterDeclarationContext(connectionProviderName, "Connection Provider");
  }

  public static ParameterDeclarationContext forFunction(String functionName) {
    return new ParameterDeclarationContext(functionName, "Function");
  }

  public static ParameterDeclarationContext forRoute(String routeName) {
    return new ParameterDeclarationContext(routeName, "Route");
  }

  public ParameterDeclarationContext(String componentName, String componentType) {
    this(componentName, componentType, false);
  }

  public ParameterDeclarationContext(String componentName, String componentType, boolean keyResolverAvailable) {
    this.componentName = componentName;
    this.componentType = componentType;
    this.keyResolverAvailable = keyResolverAvailable;
  }

  /**
   * @return The component name: the Configuration name, Operation name, etc.
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * @return The component type: Operation, Source, etc.
   */
  public String getComponentType() {
    return componentType;
  }

  public boolean isKeyResolverAvailable() {
    return keyResolverAvailable;
  }

  public List<ModelProperty> resolveStackableTypes(ExtensionParameter extensionParameter) {
    return STACKABLE_TYPES_RESOLVER.resolveStackableProperties(extensionParameter, this);
  }
}
