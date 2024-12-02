/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.module.extension.internal.loader.parser.java.StackableTypesModelPropertyResolver.newInstance;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;

import java.util.List;

/**
 * Utility class for parameter declaration to be able to give metadata of from which component this parameter is part of.
 *
 * @since 4.0
 */
public final class ParameterDeclarationContext {

  private final String componentName;
  private final String componentType;

  private final ExtensionLoadingContext loadingContext;
  private final StackableTypesModelPropertyResolver stackableTypesResolver;

  private final boolean keyResolverAvailable;

  public static ParameterDeclarationContext forConfig(String configName, ExtensionLoadingContext loadingContext) {
    return new ParameterDeclarationContext(configName, "Configuration", loadingContext);
  }

  public static ParameterDeclarationContext forOperation(String operationName, ExtensionLoadingContext loadingContext) {
    return new ParameterDeclarationContext(operationName, "Operation", loadingContext);
  }

  public static ParameterDeclarationContext forOperation(String operationName, ExtensionLoadingContext loadingContext,
                                                         boolean keyResolverAvailable) {
    return new ParameterDeclarationContext(operationName, "Operation", loadingContext, keyResolverAvailable);
  }

  public static ParameterDeclarationContext forSource(String sourceName, ExtensionLoadingContext loadingContext) {
    return new ParameterDeclarationContext(sourceName, "Source", loadingContext);
  }

  public static ParameterDeclarationContext forSource(String sourceName, ExtensionLoadingContext loadingContext,
                                                      boolean keyResolverAvailable) {
    return new ParameterDeclarationContext(sourceName, "Source", loadingContext, keyResolverAvailable);
  }

  public static ParameterDeclarationContext forConnectionProvider(String connectionProviderName,
                                                                  ExtensionLoadingContext loadingContext) {
    return new ParameterDeclarationContext(connectionProviderName, "Connection Provider", loadingContext);
  }

  public static ParameterDeclarationContext forFunction(String functionName, ExtensionLoadingContext loadingContext) {
    return new ParameterDeclarationContext(functionName, "Function", loadingContext);
  }

  public static ParameterDeclarationContext forRoute(String routeName, ExtensionLoadingContext loadingContext) {
    return new ParameterDeclarationContext(routeName, "Route", loadingContext);
  }

  public ParameterDeclarationContext(String componentName, String componentType, ExtensionLoadingContext loadingContext) {
    this(componentName, componentType, loadingContext, false);
  }

  public ParameterDeclarationContext(String componentName, String componentType, ExtensionLoadingContext loadingContext,
                                     boolean keyResolverAvailable) {
    this.componentName = componentName;
    this.componentType = componentType;
    this.loadingContext = loadingContext;
    this.stackableTypesResolver = newInstance(loadingContext);
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

  public ExtensionLoadingContext getLoadingContext() {
    return loadingContext;
  }

  /**
   * @return whether the component has a metadata key which has a key resolver associated.
   */
  public boolean isKeyResolverAvailable() {
    return keyResolverAvailable;
  }

  public List<ModelProperty> resolveStackableTypes(ExtensionParameter extensionParameter) {
    return stackableTypesResolver.resolveStackableProperties(extensionParameter, this);
  }
}
