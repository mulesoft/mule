/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.WithAlias;
import org.mule.runtime.module.extension.internal.loader.java.type.WithParameters;

import java.util.List;

/**
 * Base class for sub delegates of {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
abstract class AbstractModelLoaderDelegate {

  protected final DefaultJavaModelLoaderDelegate loader;

  AbstractModelLoaderDelegate(DefaultJavaModelLoaderDelegate loader) {
    this.loader = loader;
  }

  protected Class<?> getExtensionType() {
    return loader.getExtensionType();
  }

  ConfigModelLoaderDelegate getConfigLoaderDelegate() {
    return loader.getConfigLoaderDelegate();
  }

  OperationModelLoaderDelegate getOperationModelLoaderDelegate() {
    return loader.getOperationLoaderDelegate();
  }

  SourceModelLoaderDelegate getSourceModelLoaderDelegate() {
    return loader.getSourceModelLoaderDelegate();
  }

  ConnectionProviderModelLoaderDelegate getConnectionProviderModelLoaderDelegate() {
    return loader.getConnectionProviderModelLoaderDelegate();
  }

  OperationModelLoaderDelegate getOperationLoaderDelegate() {
    return loader.getOperationLoaderDelegate();
  }

  FunctionModelLoaderDelegate getFunctionModelLoaderDelegate() {
    return loader.getFunctionModelLoaderDelegate();
  }

  ClassTypeLoader getTypeLoader() {
    return loader.getTypeLoader();
  }

  void processComponentConnectivity(ComponentDeclarer componentDeclarer, WithParameters component, WithAlias alias) {
    final List<ExtensionParameter> connectionParameters = component.getParametersAnnotatedWith(Connection.class);
    if (connectionParameters.isEmpty()) {
      componentDeclarer.requiresConnection(false).transactional(false);
    } else if (connectionParameters.size() == 1) {
      ExtensionParameter connectionParameter = connectionParameters.iterator().next();
      componentDeclarer.requiresConnection(true)
          .transactional(TransactionalConnection.class.isAssignableFrom(connectionParameter.getType().getDeclaringClass()))
          .withModelProperty(new ConnectivityModelProperty(connectionParameter.getType().getDeclaringClass()));
    } else if (connectionParameters.size() > 1) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "%s '%s' defines %d parameters annotated with @%s. Only one is allowed",
                                                                getComponentDeclarationTypeName(componentDeclarer
                                                                    .getDeclaration()),
                                                                alias.getAlias(),
                                                                connectionParameters.size(),
                                                                Connection.class.getSimpleName()));
    }
  }
}
